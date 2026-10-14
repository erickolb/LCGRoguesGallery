package net.chateaulore.roguesgallery;

import org.junit.Test;
import static org.junit.Assert.*;

public class RefreshScheduleTest {
    @Test public void refreshBecomesDueAtTwelveHours(){
        RefreshSchedule clock=new RefreshSchedule();clock.running(100,true);
        assertEquals(1,clock.delay(100+RefreshSchedule.INTERVAL-1));
        assertEquals(0,clock.delay(100+RefreshSchedule.INTERVAL));
    }
    @Test public void pausedTimeDoesNotCount(){
        RefreshSchedule clock=new RefreshSchedule();clock.running(0,true);clock.running(1000,false);
        assertEquals(RefreshSchedule.INTERVAL-1000,clock.delay(1000000));
        clock.running(1000000,true);
        assertEquals(RefreshSchedule.INTERVAL-2000,clock.delay(1001000));
    }
    @Test public void repeatedPlayDoesNotLoseElapsedTime(){
        RefreshSchedule clock=new RefreshSchedule();clock.running(0,true);clock.running(1000,true);
        assertEquals(RefreshSchedule.INTERVAL-2000,clock.delay(2000));
    }
    @Test public void successfulRefreshStartsAnotherFullInterval(){
        RefreshSchedule clock=new RefreshSchedule();clock.running(0,true);clock.refreshed(RefreshSchedule.INTERVAL);
        assertEquals(RefreshSchedule.INTERVAL,clock.delay(RefreshSchedule.INTERVAL));
        assertEquals(0,clock.delay(2*RefreshSchedule.INTERVAL));
    }
    @Test public void failedRefreshWaitsFifteenActiveMinutes(){
        RefreshSchedule clock=new RefreshSchedule();clock.running(0,true);clock.failed(RefreshSchedule.INTERVAL);
        assertEquals(RefreshSchedule.RETRY,clock.delay(RefreshSchedule.INTERVAL));
        clock.running(RefreshSchedule.INTERVAL+1000,false);
        assertEquals(RefreshSchedule.RETRY-1000,clock.delay(100*RefreshSchedule.INTERVAL));
    }
}
