package com.enterprise.coreapi.domain.habit.service;

import com.enterprise.coreapi.common.exception.ApiException;
import com.enterprise.coreapi.common.exception.ErrorCode;
import com.enterprise.coreapi.domain.habit.dto.*;
import com.enterprise.coreapi.domain.habit.entity.*;
import com.enterprise.coreapi.domain.habit.mapper.HabitMapper;
import com.enterprise.coreapi.domain.habit.mapper.KaizenReflectionMapper;
import com.enterprise.coreapi.domain.habit.repository.*;
import com.enterprise.coreapi.domain.user.entity.User;
import com.enterprise.coreapi.domain.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class HabitServiceImpl implements HabitService {

    private final HabitRepository habitRepository;
    private final HabitLogRepository habitLogRepository;
    private final IdentityRepository identityRepository;
    private final KaizenReflectionRepository kaizenReflectionRepository;
    private final UserRepository userRepository;
    private final IdentityService identityService;
    private final CategoryTierProgressRepository categoryTierProgressRepository;
    private final HabitMapper habitMapper;
    private final KaizenReflectionMapper kaizenReflectionMapper;

    @Override
    @Transactional
    public List<HabitResponse> getHabitsForToday(UUID userPublicId) {
        identityService.seedDefaultIdentitiesIfEmpty(userPublicId);

        List<Habit> habits = habitRepository.findAllByUser_PublicIdAndActiveTrueOrderByCreatedAtAsc(userPublicId);
        LocalDate today = LocalDate.now();
        Set<Long> completedHabitIds = habitLogRepository.findAllByUser_PublicIdAndLogDate(userPublicId, today)
                .stream()
                .filter(HabitLog::isCompleted)
                .map(log -> log.getHabit().getId())
                .collect(Collectors.toSet());

        return habits.stream()
                .map(h -> {
                    HabitResponse res = habitMapper.toResponse(h);
                    boolean isCompletedToday = completedHabitIds.contains(h.getId());
                    return new HabitResponse(
                            res.publicId(),
                            res.identityPublicId(),
                            res.identityName(),
                            res.title(),
                            res.category(),
                            res.cueTrigger(),
                            res.targetLocation(),
                            res.habitStackCurrent(),
                            res.habitStackNew(),
                            res.cravingBenefit(),
                            res.responseMicroStep(),
                            res.rewardXp(),
                            res.frequency(),
                            res.targetMinutes(),
                            res.currentStreak(),
                            res.bestStreak(),
                            res.active(),
                            isCompletedToday
                    );
                })
                .toList();
    }

    @Override
    @Transactional
    public HabitResponse createHabit(UUID userPublicId, CreateHabitRequest request) {
        User user = userRepository.findByPublicId(userPublicId)
                .orElseThrow(() -> new ApiException(ErrorCode.RESOURCE_NOT_FOUND, "Kullanıcı bulunamadı."));

        Identity identity = null;
        if (request.identityPublicId() != null && !request.identityPublicId().isBlank()) {
            try {
                UUID identityUuid = UUID.fromString(request.identityPublicId());
                identity = identityRepository.findByPublicIdAndUser_PublicId(identityUuid, userPublicId)
                        .orElse(null);
            } catch (IllegalArgumentException ignored) {
            }
        }

        HabitCategory category = HabitCategory.KARIYER;
        if (request.category() != null) {
            try {
                category = HabitCategory.valueOf(request.category().toUpperCase(Locale.ROOT));
            } catch (IllegalArgumentException ignored) {
                // Varsayılan KARIYER kullanılır
            }
        }

        if (identity == null) {
            identity = findIdentityForCategory(userPublicId, category);
        }

        Habit habit = new Habit(
                user,
                identity,
                request.title(),
                category,
                request.cueTrigger(),
                request.responseMicroStep(),
                request.rewardXp() > 0 ? request.rewardXp() : 20
        );

        habit.setHabitStackCurrent(request.habitStackCurrent());
        habit.setHabitStackNew(request.habitStackNew());
        habit.setCravingBenefit(request.cravingBenefit());
        habit.setTargetLocation(request.targetLocation() != null && !request.targetLocation().isBlank()
                ? request.targetLocation().trim()
                : "Çalışma Alanı");
        habit.setTargetMinutes(request.targetMinutes() > 0 ? request.targetMinutes() : 15);

        Habit saved = habitRepository.save(habit);
        log.info("Yeni atomik alışkanlık oluşturuldu: {} (PublicId: {}, User: {})", saved.getTitle(), saved.getPublicId(), userPublicId);

        HabitResponse res = habitMapper.toResponse(saved);
        return new HabitResponse(
                res.publicId(),
                res.identityPublicId(),
                res.identityName(),
                res.title(),
                res.category(),
                res.cueTrigger(),
                res.targetLocation(),
                res.habitStackCurrent(),
                res.habitStackNew(),
                res.cravingBenefit(),
                res.responseMicroStep(),
                res.rewardXp(),
                res.frequency(),
                res.targetMinutes(),
                res.currentStreak(),
                res.bestStreak(),
                res.active(),
                false
        );
    }

    @Override
    @Transactional
    public HabitResponse toggleHabitCompletion(UUID habitPublicId, UUID userPublicId, boolean usedTwoMinuteRule) {
        Habit habit = habitRepository.findByPublicIdAndUser_PublicId(habitPublicId, userPublicId)
                .orElseThrow(() -> new ApiException(ErrorCode.RESOURCE_NOT_FOUND, "Alışkanlık bulunamadı."));

        User user = habit.getUser();
        LocalDate today = LocalDate.now();

        Optional<HabitLog> existingLog = habitLogRepository.findByHabit_PublicIdAndLogDate(habitPublicId, today);
        boolean isNowCompleted;

        if (existingLog.isPresent()) {
            // Geri al (Uncheck)
            habitLogRepository.delete(existingLog.get());
            if (habit.getCurrentStreak() > 0) {
                habit.setCurrentStreak(habit.getCurrentStreak() - 1);
            }
            Identity targetIdentity = habit.getIdentity();
            if (targetIdentity == null) {
                targetIdentity = findIdentityForCategory(userPublicId, habit.getCategory());
            }
            if (targetIdentity != null) {
                targetIdentity.removeVote();
                identityRepository.save(targetIdentity);
            }

            // 4. Yasa: Kategori Rozetini Geri Al
            categoryTierProgressRepository.findByUser_PublicIdAndCategory(userPublicId, habit.getCategory())
                    .ifPresent(tp -> {
                        tp.decrementBadge();
                        categoryTierProgressRepository.save(tp);
                    });

            isNowCompleted = false;
            log.info("Alışkanlık tamamlama kaydı geri alındı: {} (Habit: {})", habitPublicId, habit.getTitle());
        } else {
            // Tamamla (Check)
            HabitLog logEntry = new HabitLog(habit, user, today, usedTwoMinuteRule, habit.getRewardXp());
            habitLogRepository.save(logEntry);
            habit.incrementStreak();

            // İlgili kimliğe oy ver
            Identity targetIdentity = habit.getIdentity();
            if (targetIdentity == null) {
                targetIdentity = findIdentityForCategory(userPublicId, habit.getCategory());
            }
            if (targetIdentity != null) {
                targetIdentity.addVote();
                identityRepository.save(targetIdentity);
            }

            // 4. Yasa Doyurucu Kıl: Kategori Rozeti Kazan ve Kümeyi Güncelle
            CategoryTierProgress tierProgress = categoryTierProgressRepository
                    .findByUser_PublicIdAndCategory(userPublicId, habit.getCategory())
                    .orElseGet(() -> new CategoryTierProgress(user, habit.getCategory()));
            tierProgress.incrementBadge();
            categoryTierProgressRepository.save(tierProgress);

            isNowCompleted = true;
            log.info("Alışkanlık başarıyla tamamlandı: {} (Habit: {}, Streak: {}, Kategori: {})",
                    habitPublicId, habit.getTitle(), habit.getCurrentStreak(), habit.getCategory());
        }

        Habit savedHabit = habitRepository.save(habit);
        HabitResponse res = habitMapper.toResponse(savedHabit);

        return new HabitResponse(
                res.publicId(),
                res.identityPublicId(),
                res.identityName(),
                res.title(),
                res.category(),
                res.cueTrigger(),
                res.targetLocation(),
                res.habitStackCurrent(),
                res.habitStackNew(),
                res.cravingBenefit(),
                res.responseMicroStep(),
                res.rewardXp(),
                res.frequency(),
                res.targetMinutes(),
                res.currentStreak(),
                res.bestStreak(),
                res.active(),
                isNowCompleted
        );
    }

    @Override
    @Transactional
    public DashboardSummaryResponse getDashboardSummary(UUID userPublicId) {
        List<IdentityResponse> identities = identityService.getIdentitiesForUser(userPublicId);
        List<HabitResponse> habits = getHabitsForToday(userPublicId);

        int totalHabits = habits.size();
        int completedHabits = (int) habits.stream().filter(HabitResponse::completedToday).count();
        int completionRate = totalHabits > 0 ? Math.round(((float) completedHabits / totalHabits) * 100) : 0;

        int totalEarnedXp = habits.stream()
                .filter(HabitResponse::completedToday)
                .mapToInt(HabitResponse::rewardXp)
                .sum();

        int totalVotes = identities.stream().mapToInt(IdentityResponse::totalVotes).sum();

        int maxStreak = habits.stream()
                .mapToInt(HabitResponse::currentStreak)
                .max()
                .orElse(0);

        KaizenReflectionResponse reflectionResponse = kaizenReflectionRepository
                .findByUser_PublicIdAndReflectionDate(userPublicId, LocalDate.now())
                .map(kaizenReflectionMapper::toResponse)
                .orElse(null);

        List<CategoryProgressResponse> categoryTiers = buildCategoryTiers(userPublicId);

        return new DashboardSummaryResponse(
                maxStreak,
                totalHabits,
                completedHabits,
                completionRate,
                totalEarnedXp,
                totalVotes,
                identities,
                habits,
                reflectionResponse,
                categoryTiers
        );
    }

    private List<CategoryProgressResponse> buildCategoryTiers(UUID userPublicId) {
        Map<HabitCategory, Integer> badgeMap = categoryTierProgressRepository.findAllByUser_PublicId(userPublicId)
                .stream()
                .collect(Collectors.toMap(CategoryTierProgress::getCategory, CategoryTierProgress::getTotalBadges, (a, b) -> b));

        List<CategoryProgressResponse> list = new ArrayList<>();
        for (HabitCategory cat : HabitCategory.values()) {
            int totalBadges = badgeMap.getOrDefault(cat, 0);
            BadgeTier tier = BadgeTier.fromTotalBadges(totalBadges);
            int currentTierBadges = BadgeTier.getBadgesInCurrentTier(totalBadges);
            int requiredForNext = tier.getBadgesRequiredForNext();
            BadgeTier nextTier = BadgeTier.getNextTier(tier);
            int percentage = requiredForNext > 0
                    ? Math.min(100, Math.round(((float) currentTierBadges / requiredForNext) * 100))
                    : 100;
            String icon = getCategoryIcon(cat);

            list.add(new CategoryProgressResponse(
                    cat.name(),
                    cat.getDisplayName(),
                    icon,
                    tier.name(),
                    tier.getDisplayName(),
                    tier.getIcon(),
                    currentTierBadges,
                    requiredForNext,
                    totalBadges,
                    percentage,
                    nextTier.getDisplayName(),
                    tier == BadgeTier.DIAMOND
            ));
        }
        return list;
    }

    private String getCategoryIcon(HabitCategory cat) {
        return switch (cat) {
            case KARIYER -> "💼";
            case BEDEN -> "🏃";
            case ZIHIN -> "📖";
            case SOSYAL -> "☕";
            case HOBI -> "🎨";
            case SINEMA_KULTUR -> "🎬";
            case EGLENCE_OYUN -> "🎮";
            case ODAK -> "🧘";
        };
    }

    @Override
    @Transactional
    public KaizenReflectionResponse saveDailyReflection(UUID userPublicId, KaizenReflectionRequest request) {
        User user = userRepository.findByPublicId(userPublicId)
                .orElseThrow(() -> new ApiException(ErrorCode.RESOURCE_NOT_FOUND, "Kullanıcı bulunamadı."));

        LocalDate today = LocalDate.now();
        KaizenReflection reflection = kaizenReflectionRepository
                .findByUser_PublicIdAndReflectionDate(userPublicId, today)
                .orElseGet(() -> new KaizenReflection(user, today, BigDecimal.ZERO, null, null, null));

        reflection.setWhatImprovedOnePercent(request.whatImprovedOnePercent());
        reflection.setMudaDetected(request.mudaDetected());
        reflection.setPdcaActionForTomorrow(request.pdcaActionForTomorrow());

        // Günlük skoru hesapla
        long totalHabits = habitRepository.countByUser_PublicIdAndActiveTrue(userPublicId);
        long completedToday = habitLogRepository.findAllByUser_PublicIdAndLogDate(userPublicId, today).size();
        if (totalHabits > 0) {
            BigDecimal score = BigDecimal.valueOf(completedToday)
                    .multiply(BigDecimal.valueOf(100))
                    .divide(BigDecimal.valueOf(totalHabits), 2, RoundingMode.HALF_UP);
            reflection.setScorePercent(score);
        }

        KaizenReflection saved = kaizenReflectionRepository.save(reflection);
        log.info("Kaizen PDCA günlük retrospektifi kaydedildi: {} (User: {})", today, userPublicId);
        return kaizenReflectionMapper.toResponse(saved);
    }

    @Override
    @Transactional
    public void deleteHabit(UUID userPublicId, UUID habitPublicId) {
        Habit habit = habitRepository.findByPublicIdAndUser_PublicId(habitPublicId, userPublicId)
                .orElseThrow(() -> new ApiException(ErrorCode.RESOURCE_NOT_FOUND, "Alışkanlık bulunamadı."));

        List<HabitLog> logs = habitLogRepository.findAllByHabit_PublicId(habitPublicId);
        int logCount = logs.size();

        Identity targetIdentity = habit.getIdentity();
        if (targetIdentity == null) {
            targetIdentity = findIdentityForCategory(userPublicId, habit.getCategory());
        }

        // Alışkanlık silindiğinde kimlik matrisindeki oy sayısından dinamik olarak düş
        // (En az 1 oy veya varsa tüm log sayısı kadar)
        int votesToDeduct = Math.max(1, logCount);
        if (targetIdentity != null) {
            for (int i = 0; i < votesToDeduct; i++) {
                targetIdentity.removeVote();
            }
            identityRepository.save(targetIdentity);
        }

        // İlgili kategoriden rozet sayısını dinamik olarak düş
        int badgesToDeduct = Math.max(1, logCount);
        categoryTierProgressRepository.findByUser_PublicIdAndCategory(userPublicId, habit.getCategory())
                .ifPresent(tp -> {
                    for (int i = 0; i < badgesToDeduct; i++) {
                        tp.decrementBadge();
                    }
                    categoryTierProgressRepository.save(tp);
                });

        if (logCount > 0) {
            habitLogRepository.deleteAll(logs);
        }

        habit.markDeleted("USER");
        habitRepository.save(habit);
        log.info("Alışkanlık başarıyla silindi ve kazanımları dinamik olarak düşüldü: {} (User: {}, Silinen Log: {}, Düşülen Oy: {})",
                habitPublicId, userPublicId, logCount, votesToDeduct);
    }

    @Override
    @Transactional
    public HabitResponse updateHabit(UUID userPublicId, UUID habitPublicId, UpdateHabitRequest request) {
        Habit habit = habitRepository.findByPublicIdAndUser_PublicId(habitPublicId, userPublicId)
                .orElseThrow(() -> new ApiException(ErrorCode.RESOURCE_NOT_FOUND, "Alışkanlık bulunamadı."));

        applyHabitUpdates(habit, request, userPublicId);

        Habit saved = habitRepository.save(habit);
        log.info("Alışkanlık güncellendi: {} (PublicId: {}, User: {})", saved.getTitle(), saved.getPublicId(), userPublicId);

        boolean completedToday = isHabitCompletedToday(saved.getId(), userPublicId);
        return toHabitResponse(saved, completedToday);
    }

    private void applyHabitUpdates(Habit habit, UpdateHabitRequest request, UUID userPublicId) {
        if (request == null) return;
        updateTitleAndCategory(habit, request);
        updateIdentity(habit, request.identityPublicId(), userPublicId);
        updateFourLawsFields(habit, request);
        updateOptionalFields(habit, request);
    }

    private void updateTitleAndCategory(Habit habit, UpdateHabitRequest request) {
        if (request.title() != null && !request.title().isBlank()) {
            habit.setTitle(request.title().trim());
        }
        if (request.category() != null && !request.category().isBlank()) {
            try {
                habit.setCategory(HabitCategory.valueOf(request.category().toUpperCase(Locale.ROOT)));
            } catch (IllegalArgumentException ignored) {
                // Keep existing category on invalid value
            }
        }
    }

    private void updateIdentity(Habit habit, String identityPublicId, UUID userPublicId) {
        if (identityPublicId == null || identityPublicId.isBlank()) {
            return;
        }
        try {
            UUID identityUuid = UUID.fromString(identityPublicId);
            identityRepository.findByPublicIdAndUser_PublicId(identityUuid, userPublicId)
                    .ifPresent(habit::setIdentity);
        } catch (IllegalArgumentException ignored) {
            Identity fallback = findIdentityForCategory(userPublicId, habit.getCategory());
            if (fallback != null) {
                habit.setIdentity(fallback);
            }
        }
    }

    private void updateFourLawsFields(Habit habit, UpdateHabitRequest request) {
        if (request.cueTrigger() != null && !request.cueTrigger().isBlank()) {
            habit.setCueTrigger(request.cueTrigger().trim());
        }
        if (request.targetLocation() != null && !request.targetLocation().isBlank()) {
            habit.setTargetLocation(request.targetLocation().trim());
        }
        if (request.responseMicroStep() != null && !request.responseMicroStep().isBlank()) {
            habit.setResponseMicroStep(request.responseMicroStep().trim());
        }
    }

    private void updateOptionalFields(Habit habit, UpdateHabitRequest request) {
        if (request.targetMinutes() != null && request.targetMinutes() > 0) {
            habit.setTargetMinutes(request.targetMinutes());
        }
        if (request.rewardXp() != null && request.rewardXp() > 0) {
            habit.setRewardXp(request.rewardXp());
        }
        if (request.cravingBenefit() != null) {
            habit.setCravingBenefit(request.cravingBenefit());
        }
        if (request.habitStackCurrent() != null) {
            habit.setHabitStackCurrent(request.habitStackCurrent());
        }
        if (request.habitStackNew() != null) {
            habit.setHabitStackNew(request.habitStackNew());
        }
    }

    private boolean isHabitCompletedToday(Long habitId, UUID userPublicId) {
        return habitLogRepository.findAllByUser_PublicIdAndLogDate(userPublicId, LocalDate.now())
                .stream()
                .anyMatch(logItem -> logItem.isCompleted() && logItem.getHabit().getId().equals(habitId));
    }

    private HabitResponse toHabitResponse(Habit habit, boolean isCompletedToday) {
        HabitResponse res = habitMapper.toResponse(habit);
        return new HabitResponse(
                res.publicId(),
                res.identityPublicId(),
                res.identityName(),
                res.title(),
                res.category(),
                res.cueTrigger(),
                res.targetLocation(),
                res.habitStackCurrent(),
                res.habitStackNew(),
                res.cravingBenefit(),
                res.responseMicroStep(),
                res.rewardXp(),
                res.frequency(),
                res.targetMinutes(),
                res.currentStreak(),
                res.bestStreak(),
                res.active(),
                isCompletedToday
        );
    }

    private Identity findIdentityForCategory(UUID userPublicId, HabitCategory category) {
        List<Identity> identities = identityRepository.findAllByUser_PublicIdOrderByDisplayOrderAsc(userPublicId);
        if (identities.isEmpty()) {
            return null;
        }
        String targetSnippet = switch (category) {
            case KARIYER -> "Üretken";
            case ZIHIN -> "Öğrenen";
            case BEDEN -> "Zinde";
            case SOSYAL -> "Sosyal";
            case HOBI -> "Yaratıcı";
            case EGLENCE_OYUN -> "Kaşif";
            case SINEMA_KULTUR -> "Kültür";
            case ODAK -> "Dingin";
        };
        return identities.stream()
                .filter(i -> i.getName().contains(targetSnippet))
                .findFirst()
                .orElse(identities.get(0));
    }
}
