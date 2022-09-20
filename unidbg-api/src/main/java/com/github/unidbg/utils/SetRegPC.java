package com.github.unidbg.utils;

import com.github.unidbg.Emulator;
import unicorn.ArmConst;

public class SetRegPC {
    public static long setArmRegPcValue(Emulator emulator, long pc){
        if(emulator.is32Bit()){
            boolean  reg_psr_t = (emulator.getBackend().reg_read(ArmConst.UC_ARM_REG_CPSR).longValue() & 0x20) > 0;
            if(reg_psr_t){
                if(((pc & 1) == 0))
                    pc += 1;
            }
            else{
                if(((pc & 1) != 0))
                    pc -= 1;
            }

        }
        return pc;
    }
}
