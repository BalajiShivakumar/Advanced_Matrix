class CustomTimer {
  private long startTime = 0;
  private long endTime = 0;

  void begin() {
    startTime = System.currentTimeMillis();
  }

  void conclude() {
    if (startTime == 0) {
      System.err.println("CustomTimer.begin() must be called before CustomTimer.conclude()");
    }

    endTime = System.currentTimeMillis();
  }

  long getElapsedTime() {
    if (startTime == 0 || endTime == 0) {
      System.err.println("CustomTimer.begin() and CustomTimer.conclude() must be called before CustomTimer.getElapsedTime()");
      return -1;
    }

    return endTime - startTime;
  }
}
