package com.mycyclecoach.feature.userprofile.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

import com.mycyclecoach.feature.userprofile.domain.TrainingBackground;
import com.mycyclecoach.feature.userprofile.domain.TrainingGoals;
import com.mycyclecoach.feature.userprofile.domain.UserProfile;
import com.mycyclecoach.feature.userprofile.dto.BackgroundRequest;
import com.mycyclecoach.feature.userprofile.dto.GoalsRequest;
import com.mycyclecoach.feature.userprofile.dto.ProfileResponse;
import com.mycyclecoach.feature.userprofile.dto.UpdateProfileRequest;
import com.mycyclecoach.feature.userprofile.repository.TrainingBackgroundRepository;
import com.mycyclecoach.feature.userprofile.repository.TrainingGoalsRepository;
import com.mycyclecoach.feature.userprofile.repository.UserProfileRepository;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class UserProfileServiceImplTest {

    @Mock
    private UserProfileRepository userProfileRepository;

    @Mock
    private TrainingBackgroundRepository trainingBackgroundRepository;

    @Mock
    private TrainingGoalsRepository trainingGoalsRepository;

    @InjectMocks
    private UserProfileServiceImpl userProfileService;

    @Test
    void shouldGetProfileSuccessfully() {
        // given
        Long userId = 1L;
        UserProfile profile = UserProfile.builder()
                .id(1L)
                .userId(userId)
                .age(30)
                .weight(new BigDecimal("75.5"))
                .experienceLevel("Intermediate")
                .updatedAt(LocalDateTime.now())
                .build();

        given(userProfileRepository.findByUserId(userId)).willReturn(Optional.of(profile));

        // when
        ProfileResponse response = userProfileService.getProfile(userId);

        // then
        assertThat(response).isNotNull();
        assertThat(response.id()).isEqualTo(1L);
        assertThat(response.userId()).isEqualTo(userId);
        assertThat(response.age()).isEqualTo(30);
        assertThat(response.weight()).isEqualTo(new BigDecimal("75.5"));
        assertThat(response.experienceLevel()).isEqualTo("Intermediate");

        then(userProfileRepository).should().findByUserId(userId);
    }

    @Test
    void shouldThrowExceptionWhenGetProfileForNonExistentUser() {
        // given
        Long userId = 99L;
        given(userProfileRepository.findByUserId(userId)).willReturn(Optional.empty());

        // when / then
        assertThatThrownBy(() -> userProfileService.getProfile(userId))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("User profile not found for userId: 99");

        then(userProfileRepository).should().findByUserId(userId);
    }

    @Test
    void shouldUpdateProfileSuccessfully() {
        // given
        Long userId = 1L;
        UpdateProfileRequest request = new UpdateProfileRequest(32, new BigDecimal("78.0"), "Advanced");
        UserProfile profile = UserProfile.builder()
                .id(1L)
                .userId(userId)
                .age(30)
                .weight(new BigDecimal("75.5"))
                .experienceLevel("Intermediate")
                .updatedAt(LocalDateTime.now())
                .build();

        given(userProfileRepository.findByUserId(userId)).willReturn(Optional.of(profile));
        given(userProfileRepository.save(any(UserProfile.class))).willReturn(profile);

        // when
        userProfileService.updateProfile(userId, request);

        // then
        then(userProfileRepository).should().findByUserId(userId);
        then(userProfileRepository).should().save(profile);
        assertThat(profile.getAge()).isEqualTo(32);
        assertThat(profile.getWeight()).isEqualTo(new BigDecimal("78.0"));
        assertThat(profile.getExperienceLevel()).isEqualTo("Advanced");
    }

    @Test
    void shouldThrowExceptionWhenUpdateProfileForNonExistentUser() {
        // given
        Long userId = 99L;
        UpdateProfileRequest request = new UpdateProfileRequest(32, new BigDecimal("78.0"), "Advanced");
        given(userProfileRepository.findByUserId(userId)).willReturn(Optional.empty());

        // when / then
        assertThatThrownBy(() -> userProfileService.updateProfile(userId, request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("User profile not found for userId: 99");

        then(userProfileRepository).should().findByUserId(userId);
    }

    @Test
    void shouldSaveBackgroundForNewUser() {
        // given
        Long userId = 1L;
        BackgroundRequest request = new BackgroundRequest(5, 10, "None", "Tour de France 2020");
        TrainingBackground background =
                TrainingBackground.builder().userId(userId).build();

        given(trainingBackgroundRepository.findByUserId(userId)).willReturn(Optional.empty());
        given(trainingBackgroundRepository.save(any(TrainingBackground.class))).willReturn(background);

        // when
        userProfileService.saveBackground(userId, request);

        // then
        then(trainingBackgroundRepository).should().findByUserId(userId);
        then(trainingBackgroundRepository).should().save(any(TrainingBackground.class));
    }

    @Test
    void shouldUpdateBackgroundForExistingUser() {
        // given
        Long userId = 1L;
        BackgroundRequest request = new BackgroundRequest(7, 15, "Knee injury", "Multiple races");
        TrainingBackground existingBackground = TrainingBackground.builder()
                .id(1L)
                .userId(userId)
                .yearsTraining(5)
                .weeklyVolume(10)
                .recentInjuries("None")
                .priorEvents("Tour de France 2020")
                .updatedAt(LocalDateTime.now())
                .build();

        given(trainingBackgroundRepository.findByUserId(userId)).willReturn(Optional.of(existingBackground));
        given(trainingBackgroundRepository.save(any(TrainingBackground.class))).willReturn(existingBackground);

        // when
        userProfileService.saveBackground(userId, request);

        // then
        then(trainingBackgroundRepository).should().findByUserId(userId);
        then(trainingBackgroundRepository).should().save(existingBackground);
        assertThat(existingBackground.getYearsTraining()).isEqualTo(7);
        assertThat(existingBackground.getWeeklyVolume()).isEqualTo(15);
        assertThat(existingBackground.getRecentInjuries()).isEqualTo("Knee injury");
        assertThat(existingBackground.getPriorEvents()).isEqualTo("Multiple races");
    }

    @Test
    void shouldUpdateGoalsForNewUser() {
        // given
        Long userId = 1L;
        GoalsRequest request = new GoalsRequest("Complete a century ride");
        TrainingGoals goals = TrainingGoals.builder().userId(userId).build();

        given(trainingGoalsRepository.findByUserId(userId)).willReturn(Optional.empty());
        given(trainingGoalsRepository.save(any(TrainingGoals.class))).willReturn(goals);

        // when
        userProfileService.updateGoals(userId, request);

        // then
        then(trainingGoalsRepository).should().findByUserId(userId);
        then(trainingGoalsRepository).should().save(any(TrainingGoals.class));
    }

    @Test
    void shouldUpdateGoalsForExistingUser() {
        // given
        Long userId = 1L;
        GoalsRequest request = new GoalsRequest("Improve FTP by 20 watts");
        TrainingGoals existingGoals = TrainingGoals.builder()
                .id(1L)
                .userId(userId)
                .goals("Complete a century ride")
                .updatedAt(LocalDateTime.now())
                .build();

        given(trainingGoalsRepository.findByUserId(userId)).willReturn(Optional.of(existingGoals));
        given(trainingGoalsRepository.save(any(TrainingGoals.class))).willReturn(existingGoals);

        // when
        userProfileService.updateGoals(userId, request);

        // then
        then(trainingGoalsRepository).should().findByUserId(userId);
        then(trainingGoalsRepository).should().save(existingGoals);
        assertThat(existingGoals.getGoals()).isEqualTo("Improve FTP by 20 watts");
    }
}
