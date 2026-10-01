package com.enterprise.coreapi.domain.user.entity;

import com.enterprise.coreapi.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SQLRestriction;

import java.time.Instant;
import java.util.*;

@Entity
@Table(name = "users")
@SQLDelete(sql = "UPDATE users SET deleted = true, deleted_at = CURRENT_TIMESTAMP WHERE id = ? AND version = ?")
@SQLRestriction("deleted = false")
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class User extends BaseEntity {

    @Column(nullable = false, length = 255)
    private String email;

    @Column(name = "password_hash", length = 255)
    private String passwordHash;

    @Column(name = "first_name", nullable = false, length = 100)
    private String firstName;

    @Column(name = "last_name", nullable = false, length = 100)
    private String lastName;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private UserStatus status = UserStatus.ACTIVE;

    @Column(name = "failed_attempts", nullable = false)
    private int failedAttempts = 0;

    @Column(name = "locked_until")
    private Instant lockedUntil;

    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private Set<UserSocialAccount> socialAccounts = new HashSet<>();

    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(
            name = "users_roles",
            joinColumns = @JoinColumn(name = "user_id"),
            inverseJoinColumns = @JoinColumn(name = "role_id")
    )
    private Set<Role> roles = new HashSet<>();

    public User(String email, String passwordHash, String firstName, String lastName) {
        this.email = email.toLowerCase().trim();
        this.passwordHash = passwordHash;
        this.firstName = firstName;
        this.lastName = lastName;
        this.status = UserStatus.ACTIVE;
        this.failedAttempts = 0;
    }

    /**
     * Kullanıcının yerel şifresi bulunup bulunmadığını kontrol eder.
     * Sosyal girişle gelen kullanıcılarda false döner.
     */
    public boolean hasLocalPassword() {
        return this.passwordHash != null && !this.passwordHash.isBlank();
    }

    /**
     * Kullanıcıya yeni parola atar.
     */
    public void setPassword(String newPasswordHash) {
        this.passwordHash = Objects.requireNonNull(newPasswordHash, "Password hash cannot be null");
    }

    /**
     * Sosyal hesap federasyon bağlantısı kurar.
     */
    public void linkSocialAccount(String provider, String providerUserId, String providerEmail) {
        boolean exists = socialAccounts.stream()
                .anyMatch(sa -> sa.getProvider().equalsIgnoreCase(provider));
        if (exists) {
            throw new IllegalStateException("Provider " + provider + " is already linked to this user");
        }
        UserSocialAccount account = new UserSocialAccount(this, provider.toUpperCase(), providerUserId, providerEmail);
        this.socialAccounts.add(account);
    }

    /**
     * Sosyal hesap bağlantısını koparır. Kullanıcının başka bir oturum açma yöntemi (şifre veya başka sosyal hesap) yoksa izin verilmez.
     */
    public void unlinkSocialAccount(String provider) {
        if (!hasLocalPassword() && this.socialAccounts.size() <= 1) {
            throw new IllegalStateException("Cannot unlink the only authentication method. Please set a local password first.");
        }
        this.socialAccounts.removeIf(sa -> sa.getProvider().equalsIgnoreCase(provider));
    }

    /**
     * Başarısız oturum açma denemesini kaydeder; 5 denemede hesabı 15 dakika kilitler.
     */
    public void recordFailedLogin() {
        this.failedAttempts++;
        if (this.failedAttempts >= 5) {
            this.status = UserStatus.LOCKED;
            this.lockedUntil = Instant.now().plusSeconds(900); // 15 dakika
        }
    }

    public void resetFailedAttempts() {
        this.failedAttempts = 0;
        this.lockedUntil = null;
        if (this.status == UserStatus.LOCKED) {
            this.status = UserStatus.ACTIVE;
        }
    }

    public boolean isAccountLocked() {
        if (this.status == UserStatus.LOCKED) {
            if (this.lockedUntil != null && Instant.now().isAfter(this.lockedUntil)) {
                resetFailedAttempts();
                return false;
            }
            return true;
        }
        return false;
    }

    public void addRole(Role role) {
        this.roles.add(role);
    }

    public Set<Role> getRoles() {
        return Collections.unmodifiableSet(roles);
    }

    public Set<UserSocialAccount> getSocialAccounts() {
        return Collections.unmodifiableSet(socialAccounts);
    }
}
