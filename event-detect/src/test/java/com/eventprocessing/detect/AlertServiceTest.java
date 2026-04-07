package com.eventprocessing.detect;

import com.eventprocessing.detect.alert.AlertRepository;
import com.eventprocessing.detect.alert.AlertService;
import com.eventprocessing.detect.alert.AnomalyAlert;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AlertServiceTest {

    @Mock
    private AlertRepository repository;

    private AlertService alertService;

    @BeforeEach
    void setUp() {
        alertService = new AlertService(repository);
    }

    @Test
    void createAlertSavesCorrectFields() {
        when(repository.save(any())).thenAnswer(inv -> {
            AnomalyAlert a = inv.getArgument(0);
            a.setId(1L);
            return a;
        });

        AnomalyAlert alert = alertService.createAlert(
                "STATISTICAL", "HIGH", "order.created", "test",
                "Volume spike", "Details here");

        ArgumentCaptor<AnomalyAlert> captor = ArgumentCaptor.forClass(AnomalyAlert.class);
        verify(repository).save(captor.capture());

        AnomalyAlert saved = captor.getValue();
        assertThat(saved.getDetectorType()).isEqualTo("STATISTICAL");
        assertThat(saved.getSeverity()).isEqualTo("HIGH");
        assertThat(saved.getEventType()).isEqualTo("order.created");
        assertThat(saved.getEventSource()).isEqualTo("test");
        assertThat(saved.getTitle()).isEqualTo("Volume spike");
        assertThat(saved.getDescription()).isEqualTo("Details here");
        assertThat(saved.isResolved()).isFalse();
    }

    @Test
    void resolveAlertSetsResolvedTrue() {
        AnomalyAlert alert = new AnomalyAlert("STATISTICAL", "HIGH", "Test");
        alert.setId(1L);
        when(repository.findById(1L)).thenReturn(Optional.of(alert));
        when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        AnomalyAlert resolved = alertService.resolveAlert(1L);

        assertThat(resolved.isResolved()).isTrue();
    }

    @Test
    void getAlertThrowsWhenNotFound() {
        when(repository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> alertService.getAlert(999L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("999");
    }
}
