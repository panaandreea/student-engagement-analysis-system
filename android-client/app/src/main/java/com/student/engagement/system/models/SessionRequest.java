package com.student.engagement.system.models;

import androidx.annotation.NonNull;


public class SessionRequest {
    private String subject;
    private String studentGroup;
    private Integer cameraIndex;
    private String startTime;
    private String endTime;
    private int numberOfObservations;

    public SessionRequest() {
    }

    public SessionRequest(String subject, String studentGroup, Integer cameraIndex, String startTime, String endTime, int numberOfObservations) {
        this.subject = subject;
        this.studentGroup = studentGroup;
        this.cameraIndex = cameraIndex;
        this.startTime = startTime;
        this.endTime = endTime;
        this.numberOfObservations = numberOfObservations;
    }

    public String getSubject() {
        return subject;
    }

    public void setSubject(String subject) {
        this.subject = subject;
    }

    public String getStudentGroup() {
        return studentGroup;
    }

    public void setStudentGroup(String studentGroup) {
        this.studentGroup = studentGroup;
    }

    public Integer getCameraIndex() {
        return cameraIndex;
    }

    public void setCameraIndex(Integer cameraIndex) {
        this.cameraIndex = cameraIndex;
    }

    public String getStartTime() {
        return startTime;
    }

    public void setStartTime(String startTime) {
        this.startTime = startTime;
    }

    public String getEndTime() {
        return endTime;
    }

    public void setEndTime(String endTime) {
        this.endTime = endTime;
    }

    public int getNumberOfObservations() {
        return numberOfObservations;
    }

    public void setNumberOfObservations(int numberOfObservations) {
        this.numberOfObservations = numberOfObservations;
    }

    @NonNull
    @Override
    public String toString() {
        return "SessionRequest{" +
                "subject='" + subject + '\'' +
                ", studentGroup='" + studentGroup + '\'' +
                ", cameraIndex=" + cameraIndex +
                ", startTime=" + startTime +
                ", endTime=" + endTime +
                ", numberOfObservations=" + numberOfObservations +
                '}';
    }
}
