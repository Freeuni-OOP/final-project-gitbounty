package org.gitbounty.gitbountybackend.service.codebase.codebasemember;

import org.gitbounty.gitbountybackend.exception.CodebaseMemberNotFoundException;
import org.gitbounty.gitbountybackend.exception.DuplicateCodebaseMemberException;
import org.gitbounty.gitbountybackend.exception.UserNotFoundException;
import org.gitbounty.gitbountybackend.model.Codebase;
import org.gitbounty.gitbountybackend.model.CodebaseMember;
import org.gitbounty.gitbountybackend.model.CodebaseRole;
import org.gitbounty.gitbountybackend.model.User;
import org.gitbounty.gitbountybackend.service.User.UserService;
import org.gitbounty.gitbountybackend.service.codebase.CodebaseService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CodebaseMemberServiceTest {

    @Mock
    private CodebaseMemberRepository memberRepository;

    @Mock
    private CodebaseService codebaseService;

    @Mock
    private UserService userService;

    private CodebaseMemberService memberService;

    private Codebase sampleCodebase;
    private User sampleUser;

    private static final String REPO_NAME = "sample-repo";
    private static final String USERNAME = "bounty-hunter";

    @BeforeEach
    void setUp() {
        memberService = new CodebaseMemberService(memberRepository, codebaseService, userService);

        sampleCodebase = new Codebase();
        sampleCodebase.setId(100L);

        sampleUser = new User();
        sampleUser.setId(200L);
        sampleUser.setUsername(USERNAME);
    }

    @Nested
    class AddMemberTests {

        @Test
        void should_AddMember_When_UserIsNotAlreadyMember() {
            // Arrange
            when(codebaseService.getCodebase(REPO_NAME)).thenReturn(sampleCodebase);
            when(userService.findByUsername(USERNAME)).thenReturn(Optional.of(sampleUser));
            when(memberRepository.existsByCodebaseIdAndUserId(100L, 200L)).thenReturn(false);

            CodebaseMember expectedMember = CodebaseMember.builder()
                    .codebase(sampleCodebase)
                    .user(sampleUser)
                    .role(CodebaseRole.DEVELOPER)
                    .build();
            when(memberRepository.save(any(CodebaseMember.class))).thenReturn(expectedMember);

            // Act
            CodebaseMember result = memberService.addMember(REPO_NAME, USERNAME, CodebaseRole.DEVELOPER);

            // Assert
            assertThat(result).isNotNull();
            assertThat(result.getRole()).isEqualTo(CodebaseRole.DEVELOPER);
            verify(memberRepository, times(1)).save(any(CodebaseMember.class));
        }

        @Test
        void should_ThrowException_When_UserIsAlreadyMember() {
            // Arrange
            when(codebaseService.getCodebase(REPO_NAME)).thenReturn(sampleCodebase);
            when(userService.findByUsername(USERNAME)).thenReturn(Optional.of(sampleUser));
            when(memberRepository.existsByCodebaseIdAndUserId(100L, 200L)).thenReturn(true);

            // Act & Assert
            assertThatThrownBy(() -> memberService.addMember(REPO_NAME, USERNAME, CodebaseRole.DEVELOPER))
                    .isInstanceOf(DuplicateCodebaseMemberException.class)
                    .hasMessageContaining("already a member");

            verify(memberRepository, never()).save(any());
        }

        @Test
        void should_ThrowException_When_UserNotFound() {
            // Arrange
            when(codebaseService.getCodebase(REPO_NAME)).thenReturn(sampleCodebase);
            when(userService.findByUsername(USERNAME)).thenReturn(Optional.empty());

            // Act & Assert
            assertThatThrownBy(() -> memberService.addMember(REPO_NAME, USERNAME, CodebaseRole.DEVELOPER))
                    .isInstanceOf(UserNotFoundException.class)
                    .hasMessageContaining("User not found");

            verify(memberRepository, never()).save(any());
        }
    }

    @Nested
    class UpdateMemberRoleTests {

        @Test
        void should_UpdateRole_When_MembershipExists() {
            // Arrange
            CodebaseMember existingMember = CodebaseMember.builder()
                    .codebase(sampleCodebase)
                    .user(sampleUser)
                    .role(CodebaseRole.REPORTER)
                    .build();

            when(codebaseService.getCodebase(REPO_NAME)).thenReturn(sampleCodebase);
            when(userService.findByUsername(USERNAME)).thenReturn(Optional.of(sampleUser));
            when(memberRepository.findByCodebaseIdAndUserId(100L, 200L)).thenReturn(Optional.of(existingMember));
            when(memberRepository.save(existingMember)).thenReturn(existingMember);

            // Act
            CodebaseMember updated = memberService.updateMemberRole(REPO_NAME, USERNAME, CodebaseRole.MAINTAINER);

            // Assert
            assertThat(updated.getRole()).isEqualTo(CodebaseRole.MAINTAINER);
            verify(memberRepository).save(existingMember);
        }

        @Test
        void should_ThrowException_When_MembershipNotFoundOnUpdate() {
            // Arrange
            when(codebaseService.getCodebase(REPO_NAME)).thenReturn(sampleCodebase);
            when(userService.findByUsername(USERNAME)).thenReturn(Optional.of(sampleUser));
            when(memberRepository.findByCodebaseIdAndUserId(100L, 200L)).thenReturn(Optional.empty());

            // Act & Assert
            assertThatThrownBy(() -> memberService.updateMemberRole(REPO_NAME, USERNAME, CodebaseRole.MAINTAINER))
                    .isInstanceOf(CodebaseMemberNotFoundException.class)
                    .hasMessageContaining("Membership association not found");
        }
    }

    @Nested
    class RemoveMemberTests {

        @Test
        void should_DeleteMember_When_MembershipExists() {
            // Arrange
            CodebaseMember existingMember = new CodebaseMember();
            when(codebaseService.getCodebase(REPO_NAME)).thenReturn(sampleCodebase);
            when(userService.findByUsername(USERNAME)).thenReturn(Optional.of(sampleUser));
            when(memberRepository.findByCodebaseIdAndUserId(100L, 200L)).thenReturn(Optional.of(existingMember));

            // Act
            memberService.removeMember(REPO_NAME, USERNAME);

            // Assert
            verify(memberRepository, times(1)).delete(existingMember);
        }

        @Test
        void should_ThrowException_When_MembershipNotFoundOnRemoval() {
            // Arrange
            when(codebaseService.getCodebase(REPO_NAME)).thenReturn(sampleCodebase);
            when(userService.findByUsername(USERNAME)).thenReturn(Optional.of(sampleUser));
            when(memberRepository.findByCodebaseIdAndUserId(100L, 200L)).thenReturn(Optional.empty());

            // Act & Assert
            assertThatThrownBy(() -> memberService.removeMember(REPO_NAME, USERNAME))
                    .isInstanceOf(CodebaseMemberNotFoundException.class);
        }
    }

    @Nested
    class GetRosterTests {

        @Test
        void should_ReturnRosterList() {
            // Arrange
            when(codebaseService.getCodebase(REPO_NAME)).thenReturn(sampleCodebase);
            List<CodebaseMember> expectedRoster = List.of(new CodebaseMember(), new CodebaseMember());
            when(memberRepository.findByCodebaseId(100L)).thenReturn(expectedRoster);

            // Act
            List<CodebaseMember> standardRoster = memberService.getCodebaseRoster(REPO_NAME);

            // Assert
            assertThat(standardRoster).hasSize(2);
            verify(memberRepository, times(1)).findByCodebaseId(100L);
        }
    }
}