package com.enterprise.coreapi.domain.user.mapper;

import com.enterprise.coreapi.domain.user.dto.SocialAccountResponse;
import com.enterprise.coreapi.domain.user.dto.UserProfileResponse;
import com.enterprise.coreapi.domain.user.entity.Role;
import com.enterprise.coreapi.domain.user.entity.User;
import com.enterprise.coreapi.domain.user.entity.UserSocialAccount;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants;
import org.mapstruct.Named;
import org.mapstruct.ReportingPolicy;

import java.util.Collections;
import java.util.Set;
import java.util.stream.Collectors;

@Mapper(componentModel = MappingConstants.ComponentModel.SPRING, unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface UserMapper {

    @Mapping(target = "hasLocalPassword", expression = "java(user.hasLocalPassword())")
    @Mapping(target = "status", expression = "java(user.getStatus().name())")
    @Mapping(target = "roles", source = "roles", qualifiedByName = "mapRoleNames")
    @Mapping(target = "socialAccounts", source = "socialAccounts")
    UserProfileResponse toResponse(User user);

    SocialAccountResponse toSocialResponse(UserSocialAccount socialAccount);

    @Named("mapRoleNames")
    default Set<String> mapRoleNames(Set<Role> roles) {
        if (roles == null) return Collections.emptySet();
        return roles.stream().map(Role::getName).collect(Collectors.toSet());
    }
}
