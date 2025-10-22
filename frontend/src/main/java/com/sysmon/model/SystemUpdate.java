package com.sysmon.model;

public class SystemUpdate {
  private double cpuUsage;
  private long memTotal;
  private long memFree;
  private long memAvailable;
  private long buffers;
  private long cached;
  private long swapTotal;
  private long swapFree;
  private long netDownSpeed;
  private long netUpSpeed;
  private long diskUsed;
  private long diskTotal;

  public double getCpuUsage() {
    return cpuUsage;
  }

  public void setCpuUsage(double cpuUsage) {
    this.cpuUsage = cpuUsage;
  }

  public long getMemTotal() {
    return memTotal;
  }

  public void setMemTotal(long memTotal) {
    this.memTotal = memTotal;
  }

  public long getMemFree() {
    return memFree;
  }

  public void setMemFree(long memFree) {
    this.memFree = memFree;
  }

  public long getMemAvailable() {
    return memAvailable;
  }

  public void setMemAvailable(long memAvailable) {
    this.memAvailable = memAvailable;
  }

  public long getBuffers() {
    return buffers;
  }

  public void setBuffers(long buffers) {
    this.buffers = buffers;
  }

  public long getCached() {
    return cached;
  }

  public void setCached(long cached) {
    this.cached = cached;
  }

  public long getSwapTotal() {
    return swapTotal;
  }

  public void setSwapTotal(long swapTotal) {
    this.swapTotal = swapTotal;
  }

  public long getSwapFree() {
    return swapFree;
  }

  public void setSwapFree(long swapFree) {
    this.swapFree = swapFree;
  }

  public long getNetDownSpeed() {
    return netDownSpeed;
  }

  public void setNetDownSpeed(long netDownSpeed) {
    this.netDownSpeed = netDownSpeed;
  }

  public long getNetUpSpeed() {
    return netUpSpeed;
  }

  public void setNetUpSpeed(long netUpSpeed) {
    this.netUpSpeed = netUpSpeed;
  }

  public long getDiskUsed() {
    return diskUsed;
  }

  public void setDiskUsed(long diskUsed) {
    this.diskUsed = diskUsed;
  }

  public long getDiskTotal() {
    return diskTotal;
  }

  public void setDiskTotal(long diskTotal) {
    this.diskTotal = diskTotal;
  }
}
