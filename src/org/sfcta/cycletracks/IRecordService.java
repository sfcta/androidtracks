package org.sfcta.cycletracks;

public interface IRecordService {
	public int  getState();
	public void startRecording(TripData trip);
	public void cancelRecording();
	public long finishRecording(); // returns trip-id
	public long continueCurrentTrip();  // returns trip-id
	public void reset();
}
