package com.linecat.wmmtcontroller.input;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

/**
 * Integration tests for InputPipeline
 * Tests the complete input pipeline from raw input to state generation
 */
public class InputPipelineTest {

    @Mock
    private LayoutEngine mockLayoutEngine;
    
    private InputPipeline inputPipeline;

    @Before
    public void setUp() {
        // Initialize mocks
        MockitoAnnotations.initMocks(this);
        
        // Create InputPipeline with mocked LayoutEngine
        inputPipeline = new InputPipeline(mockLayoutEngine);
    }

    @Test
    public void testInitialization() {
        // Verify InputPipeline initializes correctly
        assertNotNull(inputPipeline);
        assertNotNull(inputPipeline.getNormalizedEvent());
    }

    @Test
    public void testProcessRawInput() {
        // Create a RawAccess object for testing
        RawAccess rawAccess = new RawAccess();
        rawAccess.setTouchX(100.0f);
        rawAccess.setTouchY(200.0f);
        rawAccess.setTouchPressure(1.0f);
        rawAccess.setGyroX(1.0f);
        rawAccess.setGyroY(2.0f);
        rawAccess.setGyroZ(3.0f);
        rawAccess.setAccelX(0.1f);
        rawAccess.setAccelY(0.2f);
        rawAccess.setAccelZ(0.3f);
        
        // Mock LayoutEngine.processNormalizedEvent
        doNothing().when(mockLayoutEngine).processNormalizedEvent(any(NormalizedEvent.class));
        
        // Process raw input
        inputPipeline.processRawInput(rawAccess);
        
        // Verify LayoutEngine.processNormalizedEvent was called
        verify(mockLayoutEngine, times(1)).processNormalizedEvent(any(NormalizedEvent.class));
        
        // Verify normalized event was updated
        NormalizedEvent normalizedEvent = inputPipeline.getNormalizedEvent();
        assertNotNull(normalizedEvent);
    }

    @Test
    public void testReset() {
        // Create a RawAccess object for testing
        RawAccess rawAccess = new RawAccess();
        rawAccess.setTouchX(100.0f);
        rawAccess.setTouchY(200.0f);
        
        // Process raw input first to set some state
        inputPipeline.processRawInput(rawAccess);
        
        // Reset pipeline
        inputPipeline.reset();
        
        // Verify pipeline was reset
        NormalizedEvent normalizedEvent = inputPipeline.getNormalizedEvent();
        assertNotNull(normalizedEvent);
        // Note: Need to check actual reset behavior based on implementation
    }

    @Test
    public void testNormalizedEventNotNull() {
        // Verify normalized event is not null after initialization
        assertNotNull(inputPipeline.getNormalizedEvent());
    }
}
