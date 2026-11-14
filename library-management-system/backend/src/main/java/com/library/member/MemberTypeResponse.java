package com.library.member;

public record MemberTypeResponse(
        Long id,
        String code,
        String name,
        int maxLoans,
        int loanDays,
        int maxRenewals,
        int finePerDay,
        int fineBlockThreshold,
        int maxHolds
) {
    public static MemberTypeResponse from(MemberType t) {
        return t == null ? null : new MemberTypeResponse(t.getId(), t.getCode(), t.getName(), t.getMaxLoans(),
                t.getLoanDays(), t.getMaxRenewals(), t.getFinePerDay(), t.getFineBlockThreshold(), t.getMaxHolds());
    }
}
