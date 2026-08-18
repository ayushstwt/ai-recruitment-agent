package com.ayshriv.recruitment.user.dto.response;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Lightweight role reference embedded in {@link UserResponse}. Carries only
 * the identity of a role to avoid serializing the full role entity.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class RoleReference {

    private Long id;

    private String name;
}