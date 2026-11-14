package com.library.settings;

import com.library.audit.AuditAction;
import com.library.audit.AuditService;
import com.library.common.exception.ResourceNotFoundException;
import com.library.member.MemberType;
import com.library.member.MemberTypeRepository;
import com.library.member.MemberTypeResponse;
import com.library.member.User;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Library rules: member types and the library-wide settings row. */
@Service
public class SettingsService {

    private final LibrarySettingsRepository settingsRepository;
    private final MemberTypeRepository memberTypeRepository;
    private final AuditService auditService;

    public SettingsService(LibrarySettingsRepository settingsRepository, MemberTypeRepository memberTypeRepository,
                           AuditService auditService) {
        this.settingsRepository = settingsRepository;
        this.memberTypeRepository = memberTypeRepository;
        this.auditService = auditService;
    }

    @Transactional(readOnly = true)
    public LibrarySettings current() {
        return settingsRepository.findById(LibrarySettings.SINGLETON_ID)
                .orElseThrow(() -> new IllegalStateException("library_settings row is missing; check migrations"));
    }

    @Transactional(readOnly = true)
    public SettingsResponse view() {
        LibrarySettings s = current();
        return new SettingsResponse(s.getHoldPickupDays(), s.getLostItemFee(), s.getReminderDaysBefore(),
                s.getDefaultMemberType().getId(),
                memberTypeRepository.findAllByOrderByIdAsc().stream().map(MemberTypeResponse::from).toList());
    }

    @Transactional
    public SettingsResponse update(SettingsRequest r, User admin) {
        LibrarySettings s = current();
        s.setHoldPickupDays(r.holdPickupDays());
        s.setLostItemFee(r.lostItemFee());
        s.setReminderDaysBefore(r.reminderDaysBefore());
        s.setDefaultMemberType(findType(r.defaultMemberTypeId()));
        auditService.record(admin, AuditAction.SETTINGS_CHANGED, "Settings", s.getId(),
                "Hold pickup " + r.holdPickupDays() + " days, lost item fee ₹" + r.lostItemFee()
                        + ", reminders " + r.reminderDaysBefore() + " days before due");
        return view();
    }

    @Transactional
    public MemberTypeResponse updateMemberType(Long id, MemberTypeRequest r, User admin) {
        MemberType t = findType(id);
        t.setName(r.name().trim());
        t.setMaxLoans(r.maxLoans());
        t.setLoanDays(r.loanDays());
        t.setMaxRenewals(r.maxRenewals());
        t.setFinePerDay(r.finePerDay());
        t.setFineBlockThreshold(r.fineBlockThreshold());
        t.setMaxHolds(r.maxHolds());
        auditService.record(admin, AuditAction.RULES_CHANGED, "MemberType", t.getId(),
                t.getName() + ": " + r.maxLoans() + " loans for " + r.loanDays() + " days, "
                        + r.maxRenewals() + " renewals, ₹" + r.finePerDay() + "/day, blocked over ₹"
                        + r.fineBlockThreshold() + ", " + r.maxHolds() + " holds");
        return MemberTypeResponse.from(t);
    }

    @Transactional(readOnly = true)
    public MemberType findType(Long id) {
        return memberTypeRepository.findById(id).orElseThrow(() -> ResourceNotFoundException.of("Member type", id));
    }
}
