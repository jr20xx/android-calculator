package cu.lt.joe.calculator.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;
import androidx.annotation.NonNull;
import java.util.ArrayList;
import cu.lt.joe.calculator.R;
import cu.lt.joe.calculator.models.HistoryItem;

public class OperationsHistoryAdapter extends ArrayAdapter<HistoryItem>
{
    private final Context context;

    public OperationsHistoryAdapter(Context context, ArrayList<HistoryItem> records)
    {
        super(context, R.layout.history_item_entry_layout, records);
        this.context = context;
    }

    @NonNull
    @Override
    public View getView(int position, View convertView, @NonNull ViewGroup parent)
    {
        ViewHolder holder;
        if (convertView == null)
        {
            convertView = LayoutInflater.from(context).inflate(R.layout.history_item_entry_layout, parent, false);
            holder = new ViewHolder();
            holder.operation = convertView.findViewById(R.id.history_item_entry_layout_operation);
            holder.result = convertView.findViewById(R.id.history_item_entry_layout_result);
            convertView.setTag(holder);
        }
        else
        {
            holder = (ViewHolder) convertView.getTag();
        }
        HistoryItem item = getItem(position);
        if (item != null)
        {
            holder.operation.setText(item.getOperation());
            holder.result.setText(String.format("=%s", item.getResult()));
        }
        return convertView;
    }

    private static class ViewHolder
    {
        TextView operation, result;
    }
}