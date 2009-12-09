package org.sfcta.cycletracks;

public interface IRecordService {
	public int  getState();
	public void startRecording(TripData trip);
	public void cancelRecording();
	public long finishRecording(); // returns trip-id
	public long getCurrentTrip();  // returns trip-id
	public void reset();
	public void setListener(RecordingActivity ra);
}
