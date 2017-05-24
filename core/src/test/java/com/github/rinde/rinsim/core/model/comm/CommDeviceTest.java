package com.github.rinde.rinsim.core.model.comm;

import com.github.rinde.rinsim.geom.Point;
import com.google.common.base.Optional;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Created by tomhouben on 23/05/2017.
 */
public class CommDeviceTest {

    static final CommUser[] COMMUSERS = {
            mock(CommUser.class),
            mock(CommUser.class),
            mock(CommUser.class)
    };

    CommDeviceBuilder[] builders;

    @Before
    public void setUp() {
        builders = new CommDeviceBuilder[COMMUSERS.length];
        CommModel commModel = CommModel
                .builder()
                .build(CommModelTest.fakeDependencies());

        for (int i = 0; i < COMMUSERS.length; i++){
            builders[i] = new CommDeviceBuilder(commModel,
                    COMMUSERS[i]);
        }
    }

    @Test(expected = IllegalArgumentException.class)
    public void testBroadcastRangeToLarge(){
        builders[0].setMaxRange(10);

        CommDevice device = builders[0].build();
        device.broadcast(mock(MessageContents.class), 15);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testBroadcastRangeNegative(){
        CommDevice device = builders[0].build();
        device.broadcast(mock(MessageContents.class), -1);
    }

    @Test
    public void testBroadcastRangeWithoutMaxRange(){
        when(COMMUSERS[0].getPosition()).thenReturn(Optional.of(new Point(0,0)));
        when(COMMUSERS[1].getPosition()).thenReturn(Optional.of(new Point(5, 0)));
        when(COMMUSERS[2].getPosition()).thenReturn(Optional.of(new Point(10, 0)));

        CommDevice device1 = builders[0].build();
        CommDevice device2 = builders[1].build();
        CommDevice device3 = builders[2].build();

        assertEquals(0, device2.getReceivedCount());
        assertEquals(0, device3.getReceivedCount());

        device1.broadcast(mock(MessageContents.class), 7);
        device1.sendMessages();
        assertEquals(1, device2.getReceivedCount());
        assertEquals(0, device3.getReceivedCount());

    }

}
