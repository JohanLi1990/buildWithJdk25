package disruptor.practice.oms.model;

public class StageCtx {

    private long t0;
    private long t1;
    private long t2;
    private long t3;
    private long t4;

    private Decision decision; // ACCEPT | REJECT
    private String rejCode;
    private String parsed; // parsedOrder, output of stage 1 business parse, simplified
    private String execResult;  // output of stage 3, simplified
    private TaskResponse taskResponse; // final output of stage 4

    public String getRejCode() {
        return rejCode;
    }

    public void setRejCode(String rejCode) {
        this.rejCode = rejCode;
    }

    public long getT0() {
        return t0;
    }

    public void setT0(long t0) {
        this.t0 = t0;
    }

    public long getT1() {
        return t1;
    }

    public void setT1(long t1) {
        this.t1 = t1;
    }

    public long getT2() {
        return t2;
    }

    public void setT2(long t2) {
        this.t2 = t2;
    }

    public long getT3() {
        return t3;
    }

    public void setT3(long t3) {
        this.t3 = t3;
    }

    public long getT4() {
        return t4;
    }

    public void setT4(long t4) {
        this.t4 = t4;
    }

    public Decision getDecision() {
        return decision;
    }

    public void setDecision(Decision decision) {
        this.decision = decision;
    }

    public String getParsed() {
        return parsed;
    }

    public void setParsed(String parsed) {
        this.parsed = parsed;
    }

    public String getExecResult() {
        return execResult;
    }

    public void setExecResult(String execResult) {
        this.execResult = execResult;
    }

    public TaskResponse getTaskResponse() {
        return taskResponse;
    }

    public void setTaskResponse(TaskResponse taskResponse) {
        this.taskResponse = taskResponse;
    }
}

