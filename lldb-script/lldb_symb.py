from audioop import add
import json
from pathlib import Path
from typing import TYPE_CHECKING
 
 
if TYPE_CHECKING:
    from lldb import *
 
import lldb
symbolList = []
 
def __lldb_init_module(debugger: 'SBDebugger', internal_dict: dict):
    debugger.HandleCommand('command script add -f lldb_symb.dumpsym dumpsym')
 
def dumpsym(debugger: 'SBDebugger', command: str, exe_ctx: 'SBExecutionContext', result: 'SBCommandReturnObject', internal_dict: dict):
    target = exe_ctx.GetTarget()
    m = target.modules
 
    for module in m:
        module:SBModule
        module_name = module.file.GetFilename()
        if command in module_name:
            symbols_array = module.get_symbols_array()
 
            for symbol in symbols_array:
 
                symbolList.append(symInfo(symbol,target))
            print(json.dumps(symbolList))
            dump_path = Path("./")
            f_name = command+"_sym.json"
            (dump_path / f_name).write_text(json.dumps(symbolList))
 
def symInfo(symbol,target):
    return {"name":symbol.name,"addr":symbol.addr.GetLoadAddress(target)}

