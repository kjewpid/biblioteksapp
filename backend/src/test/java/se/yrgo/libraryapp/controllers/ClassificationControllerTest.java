package se.yrgo.libraryapp.controllers;

import kotlin.collections.EmptyList;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import se.yrgo.libraryapp.dao.ClassificationDao;
import se.yrgo.libraryapp.entities.DdsClassification;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

class ClassificationControllerTest {

    private ClassificationDao classDao;
    private ClassificationController controller;

    @BeforeEach
    void setUp() {
        classDao = mock(ClassificationDao.class);
        controller = new ClassificationController(classDao);
    }

    @Test
    void getAvailableReturnsClassification() {
        List<DdsClassification> expected = List.of(
                new DdsClassification(5, "Computer programming, programs and data"),
                new DdsClassification(10, "Bibliography"),
                new DdsClassification(898, "Scandinavian")
        );

        when(classDao.getAvailable())
                .thenReturn(expected);

        List<DdsClassification> result = controller.getAvailable();

        assertThat(result).isEqualTo(expected);
        verify(classDao).getAvailable();
    }

    @Test
    void getAvailableReturnsEmptyListWhenNoClassificationsExist() {
        when(classDao.getAvailable())
                .thenReturn(List.of());

        assertThat(controller.getAvailable()).isEmpty();
        verify(classDao).getAvailable();
    }
}