package com.github.unidbg.debugger.ida;

import unicorn.Arm64Const;
import unicorn.ArmConst;

import java.util.LinkedHashMap;
import java.util.Map;

public class IdaRegsConst {
    public static final Map<Integer, Integer> arm32IdaToEmulator  = new LinkedHashMap<Integer, Integer>();
    static{
        arm32IdaToEmulator.put(0, ArmConst.UC_ARM_REG_R0);
        arm32IdaToEmulator.put(1, ArmConst.UC_ARM_REG_R1);
        arm32IdaToEmulator.put(2, ArmConst.UC_ARM_REG_R2);
        arm32IdaToEmulator.put(3, ArmConst.UC_ARM_REG_R3);
        arm32IdaToEmulator.put(4, ArmConst.UC_ARM_REG_R4);
        arm32IdaToEmulator.put(5, ArmConst.UC_ARM_REG_R5);
        arm32IdaToEmulator.put(6, ArmConst.UC_ARM_REG_R6);
        arm32IdaToEmulator.put(7, ArmConst.UC_ARM_REG_R7);
        arm32IdaToEmulator.put(8, ArmConst.UC_ARM_REG_R8);
        arm32IdaToEmulator.put(9, ArmConst.UC_ARM_REG_R9);
        arm32IdaToEmulator.put(10, ArmConst.UC_ARM_REG_R10);
        arm32IdaToEmulator.put(11, ArmConst.UC_ARM_REG_R11);
        arm32IdaToEmulator.put(12, ArmConst.UC_ARM_REG_R12);
        arm32IdaToEmulator.put(13, ArmConst.UC_ARM_REG_SP);
        arm32IdaToEmulator.put(14, ArmConst.UC_ARM_REG_LR);
        arm32IdaToEmulator.put(15, ArmConst.UC_ARM_REG_PC);
        arm32IdaToEmulator.put(16, ArmConst.UC_ARM_REG_CPSR);

    }

    public static final Map<Integer, Integer> arm64IdaToEmulator  = new LinkedHashMap<Integer, Integer>();
    static{
        arm64IdaToEmulator.put(0, Arm64Const.UC_ARM64_REG_X0);
        arm64IdaToEmulator.put(1, Arm64Const.UC_ARM64_REG_X1);
        arm64IdaToEmulator.put(2, Arm64Const.UC_ARM64_REG_X2);
        arm64IdaToEmulator.put(3, Arm64Const.UC_ARM64_REG_X3);
        arm64IdaToEmulator.put(4, Arm64Const.UC_ARM64_REG_X4);
        arm64IdaToEmulator.put(5, Arm64Const.UC_ARM64_REG_X5);
        arm64IdaToEmulator.put(6, Arm64Const.UC_ARM64_REG_X6);
        arm64IdaToEmulator.put(7, Arm64Const.UC_ARM64_REG_X7);
        arm64IdaToEmulator.put(8, Arm64Const.UC_ARM64_REG_X8);
        arm64IdaToEmulator.put(9, Arm64Const.UC_ARM64_REG_X9);
        arm64IdaToEmulator.put(10, Arm64Const.UC_ARM64_REG_X10);
        arm64IdaToEmulator.put(11, Arm64Const.UC_ARM64_REG_X11);
        arm64IdaToEmulator.put(12, Arm64Const.UC_ARM64_REG_X12);
        arm64IdaToEmulator.put(13, Arm64Const.UC_ARM64_REG_X13);
        arm64IdaToEmulator.put(14, Arm64Const.UC_ARM64_REG_X14);
        arm64IdaToEmulator.put(15, Arm64Const.UC_ARM64_REG_X15);
        arm64IdaToEmulator.put(16, Arm64Const.UC_ARM64_REG_X16);
        arm64IdaToEmulator.put(17, Arm64Const.UC_ARM64_REG_X17);
        arm64IdaToEmulator.put(18, Arm64Const.UC_ARM64_REG_X18);
        arm64IdaToEmulator.put(19, Arm64Const.UC_ARM64_REG_X19);
        arm64IdaToEmulator.put(20, Arm64Const.UC_ARM64_REG_X20);
        arm64IdaToEmulator.put(21, Arm64Const.UC_ARM64_REG_X21);
        arm64IdaToEmulator.put(22, Arm64Const.UC_ARM64_REG_X22);
        arm64IdaToEmulator.put(23, Arm64Const.UC_ARM64_REG_X23);
        arm64IdaToEmulator.put(24, Arm64Const.UC_ARM64_REG_X24);
        arm64IdaToEmulator.put(25, Arm64Const.UC_ARM64_REG_X25);
        arm64IdaToEmulator.put(26, Arm64Const.UC_ARM64_REG_X26);
        arm64IdaToEmulator.put(27, Arm64Const.UC_ARM64_REG_X27);
        arm64IdaToEmulator.put(28, Arm64Const.UC_ARM64_REG_X28);
        arm64IdaToEmulator.put(29, Arm64Const.UC_ARM64_REG_SP);
        arm64IdaToEmulator.put(30, Arm64Const.UC_ARM64_REG_LR);


        arm64IdaToEmulator.put(31, Arm64Const.UC_ARM64_REG_SP);
        arm64IdaToEmulator.put(32, Arm64Const.UC_ARM64_REG_PC);
        arm64IdaToEmulator.put(33, ArmConst.UC_ARM_REG_CPSR);



    }
}
