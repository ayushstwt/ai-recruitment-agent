package com.ayshriv.recruitment.user.service;

import com.ayshriv.recruitment.common.config.AppProperties;
import com.ayshriv.recruitment.common.exception.BadRequestException;
import com.ayshriv.recruitment.common.exception.ForbiddenException;
import com.ayshriv.recruitment.common.security.PasswordPolicyValidator;
import com.ayshriv.recruitment.organization.entity.Organization;
import com.ayshriv.recruitment.role.dto.response.RoleResponse;
import com.ayshriv.recruitment.role.entity.Role;
import com.ayshriv.recruitment.role.mapper.RoleMapper;
import com.ayshriv.recruitment.role.repository.RoleRepository;
import com.ayshriv.recruitment.role.service.RoleService;
import com.ayshriv.recruitment.user.dto.request.AssignRoleRequest;
import com.ayshriv.recruitment.user.dto.request.ChangePasswordRequest;
import com.ayshriv.recruitment.user.dto.request.CreateUserRequest;
import com.ayshriv.recruitment.user.dto.request.UpdateUserRequest;
import com.ayshriv.recruitment.user.dto.response.RoleReference;
import com.ayshriv.recruitment.user.dto.response.UserResponse;
import com.ayshriv.recruitment.user.dto.response.UserSummaryResponse;
import com.ayshriv.recruitment.user.entity.User;
import com.ayshriv.recruitment.user.exception.DuplicateUserException;
import com.ayshriv.recruitment.user.exception.UserNotFoundException;
import com.ayshriv.recruitment.user.mapper.UserMapper;
import com.ayshriv.recruitment.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private RoleRepository roleRepository;

    @Spy
    private UserMapper userMapper = new UserMapper();

    @Spy
    private RoleMapper roleMapper = new RoleMapper();

    private final AppProperties appProperties = new AppProperties();
    private final PasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    private PasswordPolicyValidator passwordPolicyValidator;
    private RoleService roleService;
    private UserService userService;

    @BeforeEach
    void setUp() {
        passwordPolicyValidator = new PasswordPolicyValidator(appProperties);
        roleService = new RoleService(roleRepository, roleMapper);
        userService = new UserService(userRepository, roleService, userMapper, passwordEncoder,
                passwordPolicyValidator);
    }

    private User user(Long id, Long organizationId) {
        User user = new User();
        user.setId(id);
        user.setFirstName("John");
        user.setLastName("Doe");
        user.setEmail("john@example.com");
        user.setPhone("+911234567890");
        user.setPasswordHash(passwordEncoder.encode("TemporaryPassword123!"));
        user.setJobTitle("Senior Recruiter");
        user.setOrganization(new Organization(organizationId));
        user.setActive(true);
        user.setDeleted(false);
        user.setCreatedOn(LocalDateTime.of(2026, 1, 1, 10, 0));
        user.setUpdatedOn(LocalDateTime.of(2026, 1, 1, 10, 0));
        return user;
    }

    private Role role(Long id, String name, boolean systemRole) {
        return role(id, name, systemRole, 10L);
    }

    private Role role(Long id, String name, boolean systemRole, Long organizationId) {
        Role role = new Role();
        role.setId(id);
        role.setName(name);
        role.setSystemRole(systemRole);
        role.setActive(true);
        role.setDeleted(false);
        if (!systemRole) {
            role.setOrganization(new Organization(organizationId));
        }
        return role;
    }

    private CreateUserRequest createRequest(String email) {
        return new CreateUserRequest(
                "John", "Doe", email, "+911234567890", "TemporaryPassword123!",
                "https://example.com/john.png", "Senior Recruiter", List.of(1L));
    }

    private UpdateUserRequest updateRequest(String email) {
        return new UpdateUserRequest(
                "Jane", "Smith", email, "+910987654321", "https://example.com/jane.png",
                "Recruitment Manager");
    }

    @Test
    void createUserHashesPasswordAndAssignsRoles() {
        when(userRepository.findByEmailAndOrganization(eq("john@example.com"), eq(10L)))
                .thenReturn(Optional.empty());
        when(roleRepository.findByIdAndNotDeleted(1L)).thenReturn(Optional.of(role(1L, "AGENCY_ADMIN", true)));
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        UserResponse response = userService.createUser(10L, createRequest("john@example.com"));

        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(captor.capture());
        User saved = captor.getValue();

        assertThat(response.getEmail()).isEqualTo("john@example.com");
        assertThat(response.getFirstName()).isEqualTo("John");
        assertThat(response.getRoles()).hasSize(1);
        assertThat(response.getRoles().get(0).getName()).isEqualTo("AGENCY_ADMIN");
        assertThat(saved.getPasswordHash()).isNotEqualTo("TemporaryPassword123!");
        assertThat(passwordEncoder.matches("TemporaryPassword123!", saved.getPasswordHash())).isTrue();
        assertThat(saved.getOrganizationId()).isEqualTo(10L);
    }

    @Test
    void createUserResponseNeverExposesPasswordOrHash() {
        when(userRepository.findByEmailAndOrganization(eq("john@example.com"), eq(10L)))
                .thenReturn(Optional.empty());
        when(roleRepository.findByIdAndNotDeleted(1L)).thenReturn(Optional.of(role(1L, "AGENCY_ADMIN", true)));
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        UserResponse response = userService.createUser(10L, createRequest("john@example.com"));

        assertThat(response).isNotInstanceOf(User.class);
        assertThat(hasField(response, "password")).isFalse();
        assertThat(hasField(response, "passwordHash")).isFalse();
        assertThat(hasField(response, "organization")).isFalse();
    }

    @Test
    void createUserRejectsDuplicateEmailWithinOrganization() {
        when(userRepository.findByEmailAndOrganization("john@example.com", 10L))
                .thenReturn(Optional.of(user(5L, 10L)));

        assertThatThrownBy(() -> userService.createUser(10L, createRequest("john@example.com")))
                .isInstanceOf(DuplicateUserException.class)
                .extracting(ex -> ((DuplicateUserException) ex).getCode())
                .isEqualTo("USER_ALREADY_EXISTS");
        verify(userRepository, never()).save(any());
    }

    @Test
    void createUserRejectsWeakPassword() {
        CreateUserRequest request = createRequest("john@example.com");
        request.setPassword("weak");

        assertThatThrownBy(() -> userService.createUser(10L, request))
                .isInstanceOf(BadRequestException.class)
                .extracting(ex -> ((BadRequestException) ex).getCode())
                .isEqualTo("INVALID_PASSWORD");
        verify(userRepository, never()).save(any());
    }

    @Test
    void createUserRejectsRoleOfAnotherOrganization() {
        when(userRepository.findByEmailAndOrganization(eq("john@example.com"), eq(10L)))
                .thenReturn(Optional.empty());
        when(roleRepository.findByIdAndNotDeleted(9L))
                .thenReturn(Optional.of(role(9L, "CUSTOM", false, 20L)));

        CreateUserRequest request = createRequest("john@example.com");
        request.setRoleIds(List.of(9L));

        assertThatThrownBy(() -> userService.createUser(10L, request))
                .isInstanceOf(ForbiddenException.class)
                .extracting(ex -> ((ForbiddenException) ex).getCode())
                .isEqualTo("ROLE_ACCESS_DENIED");
        verify(userRepository, never()).save(any());
    }

    @Test
    void getUserByIdReturnsMappedUser() {
        when(userRepository.findByIdAndNotDeleted(10L)).thenReturn(Optional.of(user(10L, 10L)));

        UserResponse response = userService.getUserById(10L, 10L);

        assertThat(response.getId()).isEqualTo(10L);
        assertThat(response.getEmail()).isEqualTo("john@example.com");
        assertThat(response.isActive()).isTrue();
    }

    @Test
    void getUserByIdThrowsNotFound() {
        when(userRepository.findByIdAndNotDeleted(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userService.getUserById(10L, 99L))
                .isInstanceOf(UserNotFoundException.class)
                .extracting(ex -> ((UserNotFoundException) ex).getCode())
                .isEqualTo("USER_NOT_FOUND");
    }

    @Test
    void getUserByIdThrowsForbiddenWhenUserBelongsToAnotherOrganization() {
        when(userRepository.findByIdAndNotDeleted(2L)).thenReturn(Optional.of(user(2L, 20L)));

        assertThatThrownBy(() -> userService.getUserById(10L, 2L))
                .isInstanceOf(ForbiddenException.class)
                .extracting(ex -> ((ForbiddenException) ex).getCode())
                .isEqualTo("USER_ACCESS_DENIED");
    }

    @Test
    void getUsersReturnsMappedPageScopedToOrganization() {
        Pageable pageable = PageRequest.of(0, 20);
        Page<User> page = new PageImpl<>(List.of(user(10L, 10L)), pageable, 1);
        when(userRepository.findAllByOrganization(10L, pageable)).thenReturn(page);

        Page<UserSummaryResponse> result = userService.getUsers(10L, pageable);

        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).getEmail()).isEqualTo("john@example.com");
        assertThat(result.getContent().get(0).getJobTitle()).isEqualTo("Senior Recruiter");
        verify(userRepository).findAllByOrganization(10L, pageable);
    }

    @Test
    void searchUsersDelegatesToOrganizationScopedQuery() {
        Pageable pageable = PageRequest.of(0, 20);
        Page<User> page = new PageImpl<>(List.of(user(10L, 10L)), pageable, 1);
        when(userRepository.searchUsers(eq(10L), eq("john"), eq(pageable))).thenReturn(page);

        Page<UserSummaryResponse> result = userService.searchUsers(10L, "  john  ", pageable);

        assertThat(result.getContent()).hasSize(1);
        verify(userRepository).searchUsers(10L, "john", pageable);
    }

    @Test
    void searchUsersWithBlankKeywordFallsBackToAllUsers() {
        Pageable pageable = PageRequest.of(0, 20);
        when(userRepository.findAllByOrganization(10L, pageable))
                .thenReturn(new PageImpl<>(List.of(), pageable, 0));

        userService.searchUsers(10L, "   ", pageable);

        verify(userRepository).findAllByOrganization(10L, pageable);
        verify(userRepository, never()).searchUsers(any(), any(), any());
    }

    @Test
    void updateUserAppliesProfileChanges() {
        User user = user(10L, 10L);
        when(userRepository.findByIdAndNotDeleted(10L)).thenReturn(Optional.of(user));
        when(userRepository.findByEmailAndOrganization("jane@example.com", 10L)).thenReturn(Optional.empty());
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        UserResponse response = userService.updateUser(10L, 10L, updateRequest("jane@example.com"));

        assertThat(response.getFirstName()).isEqualTo("Jane");
        assertThat(response.getLastName()).isEqualTo("Smith");
        assertThat(response.getEmail()).isEqualTo("jane@example.com");
        assertThat(response.getJobTitle()).isEqualTo("Recruitment Manager");
        assertThat(user.isActive()).isTrue();
        assertThat(user.isDeleted()).isFalse();
    }

    @Test
    void updateUserAllowsKeepingOwnEmail() {
        User user = user(10L, 10L);
        when(userRepository.findByIdAndNotDeleted(10L)).thenReturn(Optional.of(user));
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        UserResponse response = userService.updateUser(10L, 10L, updateRequest("john@example.com"));

        assertThat(response.getEmail()).isEqualTo("john@example.com");
        verify(userRepository, never()).findByEmailAndOrganization(any(), any());
    }

    @Test
    void updateUserRejectsEmailUsedByAnotherUser() {
        when(userRepository.findByIdAndNotDeleted(10L)).thenReturn(Optional.of(user(10L, 10L)));
        when(userRepository.findByEmailAndOrganization("taken@example.com", 10L))
                .thenReturn(Optional.of(user(11L, 10L)));

        assertThatThrownBy(() -> userService.updateUser(10L, 10L, updateRequest("taken@example.com")))
                .isInstanceOf(DuplicateUserException.class)
                .extracting(ex -> ((DuplicateUserException) ex).getCode())
                .isEqualTo("USER_ALREADY_EXISTS");
        verify(userRepository, never()).save(any());
    }

    @Test
    void updateUserThrowsForbiddenWhenTargetBelongsToAnotherOrganization() {
        when(userRepository.findByIdAndNotDeleted(2L)).thenReturn(Optional.of(user(2L, 20L)));

        assertThatThrownBy(() -> userService.updateUser(10L, 2L, updateRequest("jane@example.com")))
                .isInstanceOf(ForbiddenException.class)
                .extracting(ex -> ((ForbiddenException) ex).getCode())
                .isEqualTo("USER_ACCESS_DENIED");
    }

    @Test
    void activateUserActivatesWithoutDeleting() {
        User user = user(10L, 10L);
        user.setActive(false);
        when(userRepository.findByIdAndNotDeleted(10L)).thenReturn(Optional.of(user));
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        UserResponse response = userService.activateUser(10L, 10L);

        assertThat(response.isActive()).isTrue();
        assertThat(user.isActive()).isTrue();
        assertThat(user.isDeleted()).isFalse();
    }

    @Test
    void deactivateUserDeactivatesWithoutDeleting() {
        User user = user(10L, 10L);
        when(userRepository.findByIdAndNotDeleted(10L)).thenReturn(Optional.of(user));
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        UserResponse response = userService.deactivateUser(10L, 10L);

        assertThat(response.isActive()).isFalse();
        assertThat(user.isDeleted()).isFalse();
    }

    @Test
    void deactivateUserThrowsForbiddenForAnotherOrganization() {
        when(userRepository.findByIdAndNotDeleted(2L)).thenReturn(Optional.of(user(2L, 20L)));

        assertThatThrownBy(() -> userService.deactivateUser(10L, 2L))
                .isInstanceOf(ForbiddenException.class)
                .extracting(ex -> ((ForbiddenException) ex).getCode())
                .isEqualTo("USER_ACCESS_DENIED");
    }

    @Test
    void deleteUserSoftDeletes() {
        User user = user(10L, 10L);
        when(userRepository.findByIdAndNotDeleted(10L)).thenReturn(Optional.of(user));
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        userService.deleteUser(10L, 10L);

        assertThat(user.isDeleted()).isTrue();
        assertThat(user.isActive()).isFalse();
        verify(userRepository).save(user);
        verify(userRepository, never()).delete(any());
    }

    @Test
    void deleteUserThrowsForbiddenForAnotherOrganization() {
        when(userRepository.findByIdAndNotDeleted(2L)).thenReturn(Optional.of(user(2L, 20L)));

        assertThatThrownBy(() -> userService.deleteUser(10L, 2L))
                .isInstanceOf(ForbiddenException.class)
                .extracting(ex -> ((ForbiddenException) ex).getCode())
                .isEqualTo("USER_ACCESS_DENIED");
        verify(userRepository, never()).save(any());
    }

    @Test
    void deletedUserCannotBeRetrieved() {
        when(userRepository.findByIdAndNotDeleted(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userService.getUserById(10L, 99L))
                .isInstanceOf(UserNotFoundException.class);
    }

    @Test
    void changePasswordVerifiesCurrentAndStoresNewHash() {
        User user = user(10L, 10L);
        when(userRepository.findByIdAndNotDeleted(10L)).thenReturn(Optional.of(user));
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        userService.changePassword(10L, 10L,
                new ChangePasswordRequest("TemporaryPassword123!", "NewPassword123!"));

        assertThat(passwordEncoder.matches("NewPassword123!", user.getPasswordHash())).isTrue();
        assertThat(user.getPasswordHash()).isNotEqualTo("NewPassword123!");
    }

    @Test
    void changePasswordRejectsWrongCurrentPassword() {
        User user = user(10L, 10L);
        when(userRepository.findByIdAndNotDeleted(10L)).thenReturn(Optional.of(user));

        assertThatThrownBy(() -> userService.changePassword(10L, 10L,
                new ChangePasswordRequest("WrongPassword123!", "NewPassword123!")))
                .isInstanceOf(BadRequestException.class)
                .extracting(ex -> ((BadRequestException) ex).getCode())
                .isEqualTo("INVALID_PASSWORD");
        verify(userRepository, never()).save(any());
    }

    @Test
    void changePasswordRejectsWeakNewPassword() {
        User user = user(10L, 10L);
        when(userRepository.findByIdAndNotDeleted(10L)).thenReturn(Optional.of(user));

        assertThatThrownBy(() -> userService.changePassword(10L, 10L,
                new ChangePasswordRequest("TemporaryPassword123!", "weak")))
                .isInstanceOf(BadRequestException.class)
                .extracting(ex -> ((BadRequestException) ex).getCode())
                .isEqualTo("INVALID_PASSWORD");
    }

    @Test
    void assignRolesAddsRolesAndPreventsDuplicates() {
        User user = user(10L, 10L);
        when(userRepository.findByIdAndNotDeleted(10L)).thenReturn(Optional.of(user));
        when(roleRepository.findByIdAndNotDeleted(1L)).thenReturn(Optional.of(role(1L, "RECRUITER", true)));
        when(roleRepository.findByIdAndNotDeleted(3L)).thenReturn(Optional.of(role(3L, "VIEWER", true)));
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        UserResponse response = userService.assignRoles(10L, 10L, new AssignRoleRequest(List.of(1L, 3L, 1L)));

        assertThat(response.getRoles()).hasSize(2);
        assertThat(response.getRoles().stream().map(RoleReference::getName).toList())
                .containsExactly("RECRUITER", "VIEWER");
    }

    @Test
    void assignRolesRejectsCrossOrganizationRole() {
        User user = user(10L, 10L);
        when(userRepository.findByIdAndNotDeleted(10L)).thenReturn(Optional.of(user));
        when(roleRepository.findByIdAndNotDeleted(9L))
                .thenReturn(Optional.of(role(9L, "CUSTOM", false, 20L)));

        assertThatThrownBy(() -> userService.assignRoles(10L, 10L, new AssignRoleRequest(List.of(9L))))
                .isInstanceOf(ForbiddenException.class)
                .extracting(ex -> ((ForbiddenException) ex).getCode())
                .isEqualTo("ROLE_ACCESS_DENIED");
    }

    @Test
    void assignRolesThrowsForbiddenWhenUserBelongsToAnotherOrganization() {
        when(userRepository.findByIdAndNotDeleted(2L)).thenReturn(Optional.of(user(2L, 20L)));

        assertThatThrownBy(() -> userService.assignRoles(10L, 2L, new AssignRoleRequest(List.of(1L))))
                .isInstanceOf(ForbiddenException.class)
                .extracting(ex -> ((ForbiddenException) ex).getCode())
                .isEqualTo("USER_ACCESS_DENIED");
    }

    @Test
    void removeRoleRemovesTheRole() {
        Role recruiter = role(1L, "RECRUITER", true);
        User user = user(10L, 10L);
        user.getRoles().add(recruiter);
        when(userRepository.findByIdAndNotDeleted(10L)).thenReturn(Optional.of(user));
        when(roleRepository.findByIdAndNotDeleted(1L)).thenReturn(Optional.of(recruiter));
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        userService.removeRole(10L, 10L, 1L);

        assertThat(user.getRoles()).isEmpty();
    }

    @Test
    void removeRoleThrowsForbiddenWhenUserBelongsToAnotherOrganization() {
        when(userRepository.findByIdAndNotDeleted(2L)).thenReturn(Optional.of(user(2L, 20L)));

        assertThatThrownBy(() -> userService.removeRole(10L, 2L, 1L))
                .isInstanceOf(ForbiddenException.class)
                .extracting(ex -> ((ForbiddenException) ex).getCode())
                .isEqualTo("USER_ACCESS_DENIED");
    }

    @Test
    void getUserRolesReturnsOrderedRoleResponses() {
        User user = user(10L, 10L);
        user.getRoles().add(role(1L, "RECRUITER", true));
        user.getRoles().add(role(2L, "AGENCY_ADMIN", true));
        when(userRepository.findByIdAndNotDeleted(10L)).thenReturn(Optional.of(user));

        List<RoleResponse> roles = userService.getUserRoles(10L, 10L);

        assertThat(roles.stream().map(RoleResponse::getName).toList())
                .containsExactly("AGENCY_ADMIN", "RECRUITER");
    }

    private boolean hasField(Object object, String fieldName) {
        try {
            object.getClass().getDeclaredField(fieldName);
            return true;
        } catch (NoSuchFieldException e) {
            return false;
        }
    }
}