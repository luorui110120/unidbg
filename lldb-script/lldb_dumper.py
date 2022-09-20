from asyncio.log import logger
from datetime import datetime
import hashlib
import json
from pathlib import Path
from typing import TYPE_CHECKING, Dict, List
import sys
import zlib
import os,zipfile
from tempfile import TemporaryDirectory

if TYPE_CHECKING:
    from lldb import *
 
import lldb
black_list = {
    'startswith': ['/dev', '/system/fonts', '/dmabuf'],
    'endswith': ['(deleted)', '.apk', '.odex', '.vdex', '.dex', '.jar', '.art', '.oat', '.art]'],
    'includes': [],
}

 

def zipDir(dirpath, outFullName):
    """
    压缩指定文件夹
    :param dirpath: 目标文件夹路径
    :param outFullName: 压缩文件保存路径+xxxx.zip
    :return: 无
    """
    zip = zipfile.ZipFile(outFullName, "w", zipfile.ZIP_DEFLATED)
    for path, dirnames, filenames in os.walk(dirpath):
        # 去掉目标跟路径，只对目标文件夹下边的文件及文件夹进行压缩
        fpath = path.replace(dirpath, '')
 
        for filename in filenames:
            zip.write(os.path.join(path, filename), os.path.join(fpath, filename))
    zip.close()
 


def symInfo(symbol,target):
    return {"name":symbol.name,"addr":symbol.addr.GetLoadAddress(target)}
def dump_module_sym(target: 'SBTarget', moduleName):
    m = target.modules
    symbolList = []
    for module in m:
        module:SBModule
        module_name = module.file.GetFilename()
        if moduleName in module_name:
            symbols_array = module.get_symbols_array()
            for symbol in symbols_array:
                symbolList.append(symInfo(symbol,target))
    return symbolList

###读取文件
def read_file(inpath):
    fd = open(inpath, 'rb')
    fbuf=fd.read()
    fd.close()
    return fbuf 
def dump_maps_list(process: 'SBProcess'):
    pid = process.GetProcessID()
    if (pid > 0):
        path= "/proc/%d/maps" % pid
        if os.access(path, os.F_OK):
            return read_file(path).decode()
        else:
            import subprocess
            return subprocess.getoutput("adb shell cat /proc/%d/maps" % pid)
    return ""
def dump_arch_info(target: 'SBTarget'):
    triple = target.GetTriple()
    print(f'[dump_arch_info] triple => {triple}')
    # 'aarch64', 'unknown', 'linux', 'android'
    arch, vendor, sys, abi = triple.split('-')
    if arch == 'aarch64' or arch == 'arm64':
        return 'arm64le'
    elif arch == 'aarch64_be':
        return 'arm64be'
    elif arch == 'armeb':
        return 'armbe'
    elif arch == 'arm':
        return 'armle'
    else:
        return ''
 
def dump_regs(frame: 'SBFrame'):
    regs = {} # type: Dict[str, int]
    registers = None # type: List[SBValue]
    for registers in frame.GetRegisters():
        # - General Purpose Registers
        # - Floating Point Registers
        print(f'registers name => {registers.GetName()}')
        for register in registers:
            register_name = register.GetName()
            register.SetFormat(lldb.eFormatHex)
            register_value = register.GetValue()
            regs[register_name] = register_value
    print(f'regs => {json.dumps(regs, ensure_ascii=False, indent=4)}')
    return regs
 
 
def dump_memory_info(target: 'SBTarget'):
    logger.debug('start dump_memory_info')
    sections = []
    # 先查找全部分段信息
    for module in target.module_iter():
        module: SBModule
        for section in module.section_iter():
            section: SBSection
            module_name = module.file.GetFilename()
            start, end, size, name = get_section_info(target, section)
            section_info = {
                'module': module_name,
                'start': start,
                'end': end,
                'size': size,
                'name': name,
            }
            # size 好像有负数的情况 不知道是什么情况
            print(f'Appending: {name}')
            sections.append(section_info)
    return sections
 
def get_section_info(tar, sec):
    name = sec.name if sec.name is not None else ""
    if sec.GetParent().name is not None:
        name = sec.GetParent().name + "." + sec.name
    module_name = sec.addr.module.file.GetFilename()
    module_name = module_name if module_name is not None else ""
    long_name = module_name + "." + name
    return sec.GetLoadAddress(tar), (sec.GetLoadAddress(tar) + sec.size), sec.size, long_name
 
def dump_memory(process: 'SBProcess', dump_path: Path, black_list: Dict[str, List[str]], max_seg_size: int):
    logger.debug('start dump memory')
    memory_list = []
    mem_info = lldb.SBMemoryRegionInfo()
    start_addr = -1
    next_region_addr = 0
    while next_region_addr > start_addr:
        # 从内存起始位置开始获取内存信息
        err = process.GetMemoryRegionInfo(next_region_addr, mem_info) # type: SBError
        if not err.success:
            logger.warning(f'GetMemoryRegionInfo failed, {err}, break')
            break
        # 获取当前位置的结尾地址
        next_region_addr = mem_info.GetRegionEnd()
        # 如果超出上限 结束遍历
        if next_region_addr >= sys.maxsize:
            logger.info(f'next_region_addr:0x{next_region_addr:x} >= sys.maxsize, break')
            break
        # 获取当前这块内存的起始地址和结尾地址
        start = mem_info.GetRegionBase()
        end = mem_info.GetRegionEnd()
        # 很多内存块没有名字 预设一个
        region_name = 'UNKNOWN'
        # 记录分配了的内存
        if mem_info.IsMapped():
            name = mem_info.GetName()
            if name is None:
                name = ''
            mem_info_obj = {
                'start': start,
                'end': end,
                'name': name,
                'permissions': {
                    'r': mem_info.IsReadable(),
                    'w': mem_info.IsWritable(),
                    'x': mem_info.IsExecutable(),
                },
                'content_file': '',
            }
            memory_list.append(mem_info_obj)
    # 开始正式dump
    for seg_info in memory_list:
        try:
            start_addr = seg_info['start'] # type: int
            end_addr = seg_info['end'] # type: int
            region_name = seg_info['name'] # type: str
            permissions = seg_info['permissions'] # type: Dict[str, bool]
 
            # 跳过不可读 之后考虑下是不是能修改权限再读
            if seg_info['permissions']['r'] is False:
                logger.warning(f'Skip dump {region_name} permissions => {permissions}')
                continue
 
            # 超过预设大小的 跳过dump
            predicted_size = end_addr - start_addr
            if predicted_size > max_seg_size:
                logger.warning(f'Skip dump {region_name} size:0x{predicted_size:x}')
                continue
 
            skip_dump = False
 
            for rule in black_list['startswith']:
                if region_name.startswith(rule):
                    skip_dump = True
                    logger.warning(f'Skip dump {region_name} hit startswith rule:{rule}')
            if skip_dump: continue
 
            for rule in black_list['endswith']:
                if region_name.endswith(rule):
                    skip_dump = True
                    logger.warning(f'Skip dump {region_name} hit endswith rule:{rule}')
            if skip_dump: continue
 
            for rule in black_list['includes']:
                if rule in region_name:
                    skip_dump = True
                    logger.warning(f'Skip dump {region_name} hit includes rule:{rule}')
            if skip_dump: continue
 
            # 开始读取内存
            ts = datetime.now()
            err = lldb.SBError()
            seg_content = process.ReadMemory(start_addr, predicted_size, err)
            tm = (datetime.now() - ts).total_seconds()
            # 读取成功的才写入本地文件 并计算md5
            # 内存里面可能很多地方是0 所以压缩写入文件 减少占用
            if seg_content is None:
                logger.debug(f'Segment empty: @0x{start_addr:016x} {region_name} => {err}')
            else:
                logger.info(f'Dumping @0x{start_addr:016x} {tm:.2f}s size:0x{len(seg_content):x}: {region_name} {permissions}')
                compressed_seg_content = zlib.compress(seg_content)
                md5_sum = hashlib.md5(compressed_seg_content).hexdigest() + '.bin'
                seg_info['content_file'] = md5_sum
                (dump_path / md5_sum).write_bytes(compressed_seg_content)
        except Exception as e:
            # 这里好像不会出现异常 因为前面有 SBError 处理了 不过还是保留
            logger.error(f'Exception reading segment {region_name}', exc_info=e)
 
    return memory_list
 
def dumpMem(debugger: 'SBDebugger', command: str, exe_ctx: 'SBExecutionContext', result: 'SBCommandReturnObject', internal_dict: dict):
    outzipfile = 'memdump.zip'
    if len(command) > 0:
        print("command:" + command)
        outzipfile = command
        if outzipfile.find('.zip') < 0:
            outzipfile += '.zip'
        
    with TemporaryDirectory() as tmpdirname:
        dump_path = Path(tmpdirname)
        target = exe_ctx.GetTarget() # type: SBTarget
        arch_long = dump_arch_info(target)
        frame = exe_ctx.GetFrame() # type: SBFrame
        regs = dump_regs(frame)
    
        target = exe_ctx.GetTarget() # type: SBTarget
        sections = dump_memory_info(target)
    
        max_seg_size = 64 * 1024 * 1024
    # dump内存
        process = exe_ctx.GetProcess() # type: SBProcess
        segments = dump_memory(process, dump_path, black_list, max_seg_size)

        target = exe_ctx.GetTarget() # type: SBTarget
        libcsym = dump_module_sym(target, 'libc.so')

        process = exe_ctx.GetProcess() # type: SBProcess
        maps = dump_maps_list(process)

        (dump_path / "regs.json").write_text(json.dumps(regs))
        (dump_path / "sections.json").write_text(json.dumps(sections))
        (dump_path / "segments.json").write_text(json.dumps(segments))
        (dump_path / "libc.so_sym.json").write_text(json.dumps(libcsym))
        (dump_path / "process_maps.txt").write_text(maps)
        zipDir(tmpdirname, outzipfile)

def __lldb_init_module(debugger: 'SBDebugger', internal_dict: dict):
    debugger.HandleCommand('command script add -f lldb_dumper.dumpMem dumpmem')
