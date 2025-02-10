package cu.lt.joe.calculator.models;

import androidx.annotation.NonNull;

public class HistoryItem
{
    private final String operation, result;
    private final long id;

    public HistoryItem(long id, String operation, String result)
    {
        this.id = id;
        this.operation = operation;
        this.result = result;
    }

    public String getOperation()
    {
        return this.operation;
    }

    public String getResult()
    {
        return this.result;
    }

    public long getId()
    {
        return this.id;
    }

    @NonNull
    @Override
    public String toString()
    {
        return this.operation + " = " + this.result;
    }
}