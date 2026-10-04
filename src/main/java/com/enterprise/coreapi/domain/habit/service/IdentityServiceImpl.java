package com.enterprise.coreapi.domain.habit.service;

import com.enterprise.coreapi.common.exception.ApiException;
import com.enterprise.coreapi.common.exception.ErrorCode;
import com.enterprise.coreapi.domain.habit.dto.CreateIdentityRequest;
import com.enterprise.coreapi.domain.habit.dto.IdentityResponse;
import com.enterprise.coreapi.domain.habit.entity.Identity;
import com.enterprise.coreapi.domain.habit.mapper.IdentityMapper;
import com.enterprise.coreapi.domain.habit.repository.IdentityRepository;
import com.enterprise.coreapi.domain.user.entity.User;
import com.enterprise.coreapi.domain.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class IdentityServiceImpl implements IdentityService {

    private final IdentityRepository identityRepository;
    private final UserRepository userRepository;
    private final IdentityMapper identityMapper;

    @Override
    @Transactional(readOnly = true)
    public List<IdentityResponse> getIdentitiesForUser(UUID userPublicId) {
        seedDefaultIdentitiesIfEmpty(userPublicId);
        List<Identity> identities = identityRepository.findAllByUser_PublicIdOrderByDisplayOrderAsc(userPublicId);
        return identityMapper.toResponseList(identities);
    }

    @Override
    @Transactional
    public IdentityResponse createIdentity(UUID userPublicId, CreateIdentityRequest request) {
        User user = userRepository.findByPublicId(userPublicId)
                .orElseThrow(() -> new ApiException(ErrorCode.RESOURCE_NOT_FOUND, "Kullanıcı bulunamadı."));

        Identity identity = new Identity(
                user,
                request.name(),
                request.tagline(),
                request.icon() != null ? request.icon() : "🎯",
                request.color() != null ? request.color() : "#6366F1",
                request.votesThreshold() > 0 ? request.votesThreshold() : 50
        );

        Identity saved = identityRepository.save(identity);
        log.info("Yeni kimlik oluşturuldu: {} (PublicId: {}, User: {})", saved.getName(), saved.getPublicId(), userPublicId);
        return identityMapper.toResponse(saved);
    }

    @Override
    @Transactional
    public IdentityResponse castVote(UUID identityPublicId, UUID userPublicId) {
        Identity identity = identityRepository.findByPublicIdAndUser_PublicId(identityPublicId, userPublicId)
                .orElseThrow(() -> new ApiException(ErrorCode.RESOURCE_NOT_FOUND, "Kimlik bulunamadı."));

        identity.addVote();
        Identity saved = identityRepository.save(identity);
        log.debug("Kimliğe oy verildi: {} -> Toplam Oy: {}, Seviye: {}", saved.getName(), saved.getTotalVotes(), saved.getLevel());
        return identityMapper.toResponse(saved);
    }

    @Override
    @Transactional
    public void seedDefaultIdentitiesIfEmpty(UUID userPublicId) {
        User user = userRepository.findByPublicId(userPublicId)
                .orElseThrow(() -> new ApiException(ErrorCode.RESOURCE_NOT_FOUND, "Kullanıcı bulunamadı."));

        List<Identity> existing = identityRepository.findAllByUser_PublicIdOrderByDisplayOrderAsc(userPublicId);
        Set<String> existingNames = existing.stream().map(Identity::getName).collect(Collectors.toSet());

        List<Identity> toAdd = new ArrayList<>();

        if (!existingNames.contains("Üretken Profesyonel & Değer Üreten") && !existingNames.contains("Yazılım Mimarı & Problem Çözücü")) {
            Identity proIdentity = new Identity(
                    user,
                    "Üretken Profesyonel & Değer Üreten",
                    "Yaptığı işe özen gösterir, odaklanır ve her gün somut değer katar",
                    "💼",
                    "#6366F1",
                    50
            );
            proIdentity.setDisplayOrder(1);
            toAdd.add(proIdentity);
        }

        if (!existingNames.contains("Sürekli Öğrenen & Düşünür")) {
            Identity readerIdentity = new Identity(
                    user,
                    "Sürekli Öğrenen & Düşünür",
                    "Her gün yeni bir kavram öğrenir ve zihnini keskin tutar",
                    "📖",
                    "#A855F7",
                    40
            );
            readerIdentity.setDisplayOrder(2);
            toAdd.add(readerIdentity);
        }

        if (!existingNames.contains("Zinde ve Enerjik Birey")) {
            Identity healthIdentity = new Identity(
                    user,
                    "Zinde ve Enerjik Birey",
                    "Bedenine saygı duyar, hareket eder ve berrak bir zihin korur",
                    "🏃",
                    "#10B981",
                    35
            );
            healthIdentity.setDisplayOrder(3);
            toAdd.add(healthIdentity);
        }

        if (!existingNames.contains("Sosyal & Paylaşımcı Dost")) {
            Identity socialIdentity = new Identity(
                    user,
                    "Sosyal & Paylaşımcı Dost",
                    "İnsan ilişkilerine değer verir, sevdikleriyle bağlarını sıcak tutar",
                    "☕",
                    "#F59E0B",
                    30
            );
            socialIdentity.setDisplayOrder(4);
            toAdd.add(socialIdentity);
        }

        if (!existingNames.contains("Yaratıcı & Çok Yönlü Ruh")) {
            Identity creativeIdentity = new Identity(
                    user,
                    "Yaratıcı & Çok Yönlü Ruh",
                    "Hobilerine, sanatına ve üretken tutkularına özenle vakit ayırır",
                    "🎨",
                    "#EC4899",
                    30
            );
            creativeIdentity.setDisplayOrder(5);
            toAdd.add(creativeIdentity);
        }

        if (!existingNames.contains("Yaşamın Tadını Çıkaran Kaşif")) {
            Identity explorerIdentity = new Identity(
                    user,
                    "Yaşamın Tadını Çıkaran Kaşif",
                    "Oyunlar, eğlence ve neşeyle zihnini dinlendirir, dengeli yaşar",
                    "🎮",
                    "#8B5CF6",
                    30
            );
            explorerIdentity.setDisplayOrder(6);
            toAdd.add(explorerIdentity);
        }

        if (!existingNames.contains("Kültür Sanat & Sinema Tutkunu")) {
            Identity cultureIdentity = new Identity(
                    user,
                    "Kültür Sanat & Sinema Tutkunu",
                    "Sinema, tiyatro ve sanatla vizyonunu genişletir; hikayelerden ilham alır",
                    "🎬",
                    "#F43F5E",
                    30
            );
            cultureIdentity.setDisplayOrder(7);
            toAdd.add(cultureIdentity);
        }

        if (!existingNames.contains("Bilinçli ve Dingin Zihin")) {
            Identity mindfulIdentity = new Identity(
                    user,
                    "Bilinçli ve Dingin Zihin",
                    "Nefesine ve ana odaklanır, zihinsel dinginlik ve berraklık kazanır",
                    "🧘",
                    "#14B8A6",
                    30
            );
            mindfulIdentity.setDisplayOrder(8);
            toAdd.add(mindfulIdentity);
        }

        if (!toAdd.isEmpty()) {
            identityRepository.saveAll(toAdd);
            log.info("Kullanıcı için dengeli yaşam James Clear kimlikleri güncellendi: {} adet eklendi (User: {})", toAdd.size(), userPublicId);
        }
    }
}
