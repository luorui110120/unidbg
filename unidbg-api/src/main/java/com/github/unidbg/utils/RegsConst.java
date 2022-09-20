package com.github.unidbg.utils;

import unicorn.Arm64Const;
import unicorn.ArmConst;

import java.util.LinkedHashMap;
import java.util.Map;

public class RegsConst {
    public static final  Map<String, Integer> arm64Regs  = new LinkedHashMap<String, Integer>();
    static{
        arm64Regs.put("x0", Arm64Const.UC_ARM64_REG_X0);
        arm64Regs.put("x1", Arm64Const.UC_ARM64_REG_X1);
        arm64Regs.put("x2", Arm64Const.UC_ARM64_REG_X2);
        arm64Regs.put("x3", Arm64Const.UC_ARM64_REG_X3);
        arm64Regs.put("x4", Arm64Const.UC_ARM64_REG_X4);
        arm64Regs.put("x5", Arm64Const.UC_ARM64_REG_X5);
        arm64Regs.put("x6", Arm64Const.UC_ARM64_REG_X6);
        arm64Regs.put("x7", Arm64Const.UC_ARM64_REG_X7);
        arm64Regs.put("x8", Arm64Const.UC_ARM64_REG_X8);
        arm64Regs.put("x9", Arm64Const.UC_ARM64_REG_X9);
        arm64Regs.put("x10", Arm64Const.UC_ARM64_REG_X10);
        arm64Regs.put("x11", Arm64Const.UC_ARM64_REG_X11);
        arm64Regs.put("x12", Arm64Const.UC_ARM64_REG_X12);
        arm64Regs.put("x13", Arm64Const.UC_ARM64_REG_X13);
        arm64Regs.put("x14", Arm64Const.UC_ARM64_REG_X14);
        arm64Regs.put("x15", Arm64Const.UC_ARM64_REG_X15);
        arm64Regs.put("x16", Arm64Const.UC_ARM64_REG_X16);
        arm64Regs.put("x17", Arm64Const.UC_ARM64_REG_X17);
        arm64Regs.put("x18", Arm64Const.UC_ARM64_REG_X18);
        arm64Regs.put("x19", Arm64Const.UC_ARM64_REG_X19);
        arm64Regs.put("x20", Arm64Const.UC_ARM64_REG_X20);
        arm64Regs.put("x21", Arm64Const.UC_ARM64_REG_X21);
        arm64Regs.put("x22", Arm64Const.UC_ARM64_REG_X22);
        arm64Regs.put("x23", Arm64Const.UC_ARM64_REG_X23);
        arm64Regs.put("x24", Arm64Const.UC_ARM64_REG_X24);
        arm64Regs.put("x25", Arm64Const.UC_ARM64_REG_X25);
        arm64Regs.put("x26", Arm64Const.UC_ARM64_REG_X26);
        arm64Regs.put("x27", Arm64Const.UC_ARM64_REG_X27);
        arm64Regs.put("x28", Arm64Const.UC_ARM64_REG_X28);

        arm64Regs.put("fp", Arm64Const.UC_ARM64_REG_FP);
        arm64Regs.put("lr", Arm64Const.UC_ARM64_REG_LR);
        arm64Regs.put("sp", Arm64Const.UC_ARM64_REG_SP);
        arm64Regs.put("pc", Arm64Const.UC_ARM64_REG_PC);
        arm64Regs.put("cpsr", ArmConst.UC_ARM_REG_CPSR);


    }

    public static final  Map<String, Integer> arm32Regs  = new LinkedHashMap<String, Integer>();
    static{
        arm32Regs.put("r0", ArmConst.UC_ARM_REG_R0);
        arm32Regs.put("r1", ArmConst.UC_ARM_REG_R1);
        arm32Regs.put("r2", ArmConst.UC_ARM_REG_R2);
        arm32Regs.put("r3", ArmConst.UC_ARM_REG_R3);
        arm32Regs.put("r4", ArmConst.UC_ARM_REG_R4);
        arm32Regs.put("r5", ArmConst.UC_ARM_REG_R5);
        arm32Regs.put("r6", ArmConst.UC_ARM_REG_R6);
        arm32Regs.put("r7", ArmConst.UC_ARM_REG_R7);
        arm32Regs.put("r8", ArmConst.UC_ARM_REG_R8);
        arm32Regs.put("r9", ArmConst.UC_ARM_REG_R9);
        arm32Regs.put("r10", ArmConst.UC_ARM_REG_R10);
        arm32Regs.put("r11", ArmConst.UC_ARM_REG_R11);
        arm32Regs.put("r12", ArmConst.UC_ARM_REG_R12);

        arm32Regs.put("lr", ArmConst.UC_ARM_REG_LR);
        arm32Regs.put("sp", ArmConst.UC_ARM_REG_SP);
        arm32Regs.put("pc", ArmConst.UC_ARM_REG_PC);
        arm32Regs.put("cpsr", ArmConst.UC_ARM_REG_CPSR);


    }
}
