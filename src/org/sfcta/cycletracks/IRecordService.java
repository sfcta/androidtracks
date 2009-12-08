package org.sfcta.cycletracks;

public interface IRecordService {
	public int getState();
	public void startRecording(TripData trip);
	public void cancelRecording();
}
