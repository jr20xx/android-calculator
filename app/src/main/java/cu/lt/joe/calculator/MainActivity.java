package cu.lt.joe.calculator;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Bundle;
import android.view.ActionMode;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.appcompat.widget.PopupMenu;
import androidx.core.view.GravityCompat;
import androidx.databinding.DataBindingUtil;
import androidx.preference.PreferenceManager;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.snackbar.Snackbar;
import java.io.PrintWriter;
import java.io.StringWriter;
import cu.lt.joe.calculator.adapters.OperationsHistoryAdapter;
import cu.lt.joe.calculator.databinding.MainLayoutBinding;
import cu.lt.joe.calculator.db.HistoryDatabaseHandler;
import cu.lt.joe.calculator.models.HistoryItem;
import cu.lt.joe.jcalc.JCalc;
import cu.lt.joe.jcalc.exceptions.InfiniteResultException;
import cu.lt.joe.jcalc.exceptions.NotNumericResultException;
import cu.lt.joe.jcalc.exceptions.UnbalancedParenthesesException;

public class MainActivity extends AppCompatActivity implements View.OnClickListener, View.OnLongClickListener
{
    private MainLayoutBinding binding;
    private boolean solved;
    private HistoryDatabaseHandler operations_records_handler;
    private SharedPreferences sharp;
    private SharedPreferences.Editor editor;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setRequestedOrientation(android.content.pm.ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        sharp = PreferenceManager.getDefaultSharedPreferences(this);
        editor = sharp.edit();
        AppCompatDelegate.setDefaultNightMode(sharp.getBoolean("UI_MODE_DARK", false) ? AppCompatDelegate.MODE_NIGHT_NO : AppCompatDelegate.MODE_NIGHT_YES);
        binding = DataBindingUtil.setContentView(this, R.layout.main_layout);
        binding.historyLv.setDividerHeight(0);
        if (Build.VERSION.SDK_INT > Build.VERSION_CODES.KITKAT_WATCH)
            binding.buttonsLayout.screen.setShowSoftInputOnFocus(false);
        binding.buttonsLayout.screen.setCursorVisible(false);
        solved = getIntent().getBooleanExtra("solved", false);
        operations_records_handler = new HistoryDatabaseHandler(this, () ->
        {
            binding.historyLv.setAdapter(new OperationsHistoryAdapter(MainActivity.this, operations_records_handler.getOperationsHistory()));
            solved = true;
        });
        binding.historyLv.setAdapter(new OperationsHistoryAdapter(MainActivity.this, operations_records_handler.getOperationsHistory()));
        binding.buttonsLayout.screen.setCustomSelectionActionModeCallback(new ActionMode.Callback()
        {
            @Override
            public boolean onCreateActionMode(ActionMode p1, Menu p2)
            {
                return false;
            }

            @Override
            public boolean onPrepareActionMode(ActionMode p1, Menu p2)
            {
                return false;
            }

            @Override
            public boolean onActionItemClicked(ActionMode p1, MenuItem p2)
            {
                return false;
            }

            @Override
            public void onDestroyActionMode(ActionMode p1)
            {
            }
        });

        binding.buttonsLayout.screen.setOnKeyListener((v, keyCode, event) ->
        {
            if (event.getKeyCode() == KeyEvent.KEYCODE_BACK)
                return false;
            return true;
        });

        binding.historyLv.setOnItemClickListener((p1, p2, p3, p4) ->
        {
            binding.historyDrawer.closeDrawer(GravityCompat.END);
            solved = false;
            binding.buttonsLayout.screen.setText(((HistoryItem) p1.getItemAtPosition(p3)).getResult());
        });
        binding.historyLv.setOnItemLongClickListener((p1, p2, p3, p4) ->
        {
            showHistoryItemPopupMenu((HistoryItem) p1.getItemAtPosition(p3), p2);
            return true;
        });
        binding.buttonsLayout.delete.setOnLongClickListener(this);
        binding.buttonsLayout.screen.setOnLongClickListener(this);
        binding.buttonsLayout.setHandlerActivity(this);
    }

    public void onNumberClick(View button)
    {
        String screenContent = binding.buttonsLayout.screen.getText().toString();
        String number = ((Button) button).getText().toString();
        if (!screenContent.isEmpty())
        {
            char lastChar = screenContent.charAt(screenContent.length() - 1);
            if (solved)
            {
                solved = false;
                if (lastChar == '(' || lastChar == '.')
                    binding.buttonsLayout.screen.setText(String.format("%s%s", screenContent, number));
                else
                    binding.buttonsLayout.screen.setText(number);
            }
            else if (lastChar == ')')
                binding.buttonsLayout.screen.setText(String.format("%s×%s", screenContent, number));
            else
                binding.buttonsLayout.screen.setText(String.format("%s%s", screenContent, number));
        }
        else
            binding.buttonsLayout.screen.setText(number);
        binding.buttonsLayout.screen.setSelection(binding.buttonsLayout.screen.getText().length());
    }

    public void onOperatorClick(View button)
    {
        String screenContent = binding.buttonsLayout.screen.getText().toString();
        String operator = ((Button) button).getText().toString();
        solved = false;
        if (!screenContent.isEmpty())
        {
            char lastChar = screenContent.charAt(screenContent.length() - 1);
            if (isNumber(lastChar + "") || lastChar == ')')
                screenContent += operator;
            else if (lastChar == '.')
                screenContent += "0" + operator;
            else if (lastChar == '(' && operator.equals("-"))
                screenContent += operator;
            else if (screenContent.endsWith("(-"))
                screenContent = operator.equals("+") ? screenContent.substring(0, screenContent.length() - 1) : screenContent;
            else if (isOperator(lastChar + ""))
                screenContent = screenContent.substring(0, screenContent.length() - 1) + operator;
            binding.buttonsLayout.screen.setText(screenContent);
            binding.buttonsLayout.screen.setSelection(binding.buttonsLayout.screen.getText().length());
        }
    }

    @Override
    public void onClick(View view)
    {
        String text = binding.buttonsLayout.screen.getText().toString();
        char lastChar = !text.isEmpty() ? text.charAt(text.length() - 1) : ' ';
        if (view.equals(binding.buttonsLayout.comma))
        {
            if (!text.isEmpty())
            {
                if (isNumber(lastChar + ""))
                {
                    String arr[] = text.replace("(", "( ").replace("+", " + ").replace("-", " - ").replace("×", " × ").replace("÷", " ÷ ").replace("^", " ^ ").replace(")", " )").replace("n", "-").trim().split(" ");
                    if (!arr[arr.length - 1].contains("."))
                        binding.buttonsLayout.screen.setText(String.format("%s.", binding.buttonsLayout.screen.getText()));
                }
                else if (isOperator(lastChar + "") || lastChar == '(')
                    binding.buttonsLayout.screen.setText(String.format("%s0.", binding.buttonsLayout.screen.getText()));
                binding.buttonsLayout.screen.setSelection(binding.buttonsLayout.screen.getText().length());
            }
        }
        else if (view.equals(binding.buttonsLayout.delete))
        {
            if (solved)
            {
                solved = false;
                binding.buttonsLayout.screen.setText("");
            }
            else if (!text.isEmpty())
            {
                text = text.substring(0, text.length() - 1);
                binding.buttonsLayout.screen.setText(text);
                binding.buttonsLayout.screen.setSelection(text.length());
            }
        }
        else if (view.equals(binding.buttonsLayout.openParentheses))
        {
            if (!text.isEmpty() && (isNumber(lastChar + "") || lastChar == ')'))
            {
                binding.buttonsLayout.screen.setText(String.format("%s×(", text));
                binding.buttonsLayout.screen.setSelection(binding.buttonsLayout.screen.getText().length());
            }
            else
            {
                binding.buttonsLayout.screen.setText(String.format("%s(", text));
                binding.buttonsLayout.screen.setSelection(binding.buttonsLayout.screen.getText().length());
            }
        }
        else if (view.equals(binding.buttonsLayout.closeParentheses))
        {
            if (text.contains("("))
            {
                binding.buttonsLayout.screen.setText(text += lastChar == '(' ? "1)" : ")");
                binding.buttonsLayout.screen.setSelection(text.length());
            }
        }
        else if (view.equals(binding.buttonsLayout.equal))
        {
            if (!text.isEmpty())
            {
                try
                {
                    if (text.contains("+") || text.contains("-") || text.contains("×") || text.contains("÷") || text.contains("^"))
                    {
                        String result = JCalc.solveMathExpression(text, true);
                        binding.buttonsLayout.screen.setText(result);
                        binding.buttonsLayout.screen.setSelection(0);
                        operations_records_handler.saveOperation(text, result);
                    }
                }
                catch (NotNumericResultException e)
                {
                    binding.buttonsLayout.screen.setText("");
                    Snackbar.make(binding.buttonsLayout.screen, R.string.mistcalc, Snackbar.LENGTH_LONG).setAction(R.string.more_details, v -> showDetails(MainActivity.this, "NULO")).show();
                }
                catch (InfiniteResultException e)
                {
                    binding.buttonsLayout.screen.setText("");
                    Snackbar.make(binding.buttonsLayout.screen, R.string.mistcalc, Snackbar.LENGTH_LONG).setAction(R.string.more_details, v -> showDetails(MainActivity.this, "INF")).show();
                }
                catch (UnbalancedParenthesesException x)
                {
                    binding.buttonsLayout.screen.setText("");
                    Snackbar.make(binding.buttonsLayout.screen, R.string.mistcalc, Snackbar.LENGTH_LONG).setAction(R.string.more_details, v -> showDetails(MainActivity.this, "PIE")).show();
                }
                catch (Exception e)
                {
                    Throwable t = new Throwable(e);
                    StringWriter result = new StringWriter();
                    PrintWriter printer = new PrintWriter(result);
                    while (t != null)
                    {
                        t.printStackTrace(printer);
                        t = t.getCause();
                    }
                    final String exception = result.toString();
                    Snackbar.make(binding.buttonsLayout.screen, R.string.mistcalc, Snackbar.LENGTH_LONG).setAction(R.string.more_details, v -> showDetails(MainActivity.this, exception)).show();
                }
            }
        }
        else if (view.equals(binding.buttonsLayout.toggleHistoryDrawer))
        {
            binding.historyDrawer.openDrawer(GravityCompat.END);
        }
        else if (view.equals(binding.clearHistoryButton))
        {
            operations_records_handler.clearRecords();
        }
        else if (view.equals(binding.buttonsLayout.toggleDayNightMode))
        {
            editor.putBoolean("UI_MODE_DARK", !sharp.getBoolean("UI_MODE_DARK", false)).commit();
            startActivity(new Intent(getApplicationContext(), MainActivity.class).setFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION).setFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK).setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP).putExtra("solved", solved).putExtra("screen_content", binding.buttonsLayout.screen.getText().toString()));
            overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
        }
    }

    @Override
    public boolean onLongClick(View v)
    {
        if (v.equals(binding.buttonsLayout.delete))
        {
            binding.buttonsLayout.screen.setText("");
            return true;
        }
        else if (v.equals(binding.buttonsLayout.screen))
        {
            if (!binding.buttonsLayout.screen.getText().toString().isEmpty())
            {
                final BottomSheetDialog dialog = new BottomSheetDialog(MainActivity.this);
                View edits = getLayoutInflater().inflate(R.layout.screen_menu, null);
                dialog.setContentView(edits);
                edits.findViewById(R.id.copy_lay).setOnClickListener(view ->
                {
                    copyToClipboard(null, binding.buttonsLayout.screen.getText().toString());
                    dialog.dismiss();
                    Snackbar.make(binding.buttonsLayout.screen, R.string.copied, Snackbar.LENGTH_SHORT).show();
                });
                edits.findViewById(R.id.cut_lay).setOnClickListener(view ->
                {
                    solved = false;
                    copyToClipboard(null, binding.buttonsLayout.screen.getText().toString());
                    dialog.dismiss();
                    binding.buttonsLayout.screen.setText("");
                    Snackbar.make(binding.buttonsLayout.screen, R.string.cutText, Snackbar.LENGTH_SHORT).show();
                });
                dialog.show();
            }
            return true;
        }
        return false;
    }

    private boolean copyToClipboard(@Nullable String title, @NonNull String description)
    {
        try
        {
            ClipboardManager cm = (ClipboardManager) MainActivity.this.getSystemService(Context.CLIPBOARD_SERVICE);
            ClipData cd = ClipData.newPlainText(title, description);
            cm.setPrimaryClip(cd);
            return true;
        }
        catch (Exception ignored)
        {
        }
        return false;
    }

    private void showHistoryItemPopupMenu(final HistoryItem item, View view)
    {
        PopupMenu menu = new PopupMenu(MainActivity.this, view);
        menu.inflate(R.menu.records_item_menu);
        menu.setOnMenuItemClickListener(p1 ->
        {
            switch (p1.getItemId())
            {
                case R.id.copy_operation:
                {
                    copyToClipboard(null, item.getOperation());
                    return true;
                }
                case R.id.copy_result:
                {
                    copyToClipboard(null, item.getResult().replace("=", ""));
                    return true;
                }
                case R.id.delete_history_item:
                {
                    operations_records_handler.deleteOperation(item.getId());
                    return true;
                }
                default:
                    return false;
            }
        });
        menu.setGravity(Gravity.END);
        menu.show();
    }

    private Boolean isNumber(String x)
    {
        for (int i = 0; i <= 9; i++)
        {
            if (Integer.toString(i).equals(x))
                return true;
        }
        return false;
    }

    @NonNull
    private Boolean isOperator(String x)
    {
        return x.equals("+") || x.equals("-") || x.equals("×") || x.equals("÷") || x.equals("^");
    }

    private void showDetails(Context context, String type)
    {
        final BottomSheetDialog detError = new BottomSheetDialog(context);
        View inflado = getLayoutInflater().inflate(R.layout.error_dialog, null);
        detError.setContentView(inflado);
        final TextView head = inflado.findViewById(R.id.title);
        final Button send = inflado.findViewById(R.id.SEND);
        final TextView errorT = inflado.findViewById(R.id.errorText);
        send.setVisibility(View.GONE);
        switch (type)
        {
            case "PIE":
            {
                errorT.setText(R.string.missbalanced);
                break;
            }
            case "NULO":
            {
                errorT.setText(R.string.nan_error);
                break;
            }
            case "INF":
            {
                errorT.setText(R.string.infinite_error);
                break;
            }
            default:
            {
                errorT.setText(R.string.genericE + ": " + type);
                send.setVisibility(View.VISIBLE);
                break;
            }
        }
        send.setOnClickListener(v ->
        {
            detError.dismiss();
            startActivity(Intent.createChooser(new Intent(Intent.ACTION_SEND).setType("text/plain").putExtra(Intent.EXTRA_TEXT, errorT.getText().toString()).putExtra(Intent.EXTRA_SUBJECT, head.getText().toString()), getResources().getString(R.string.sendTit)));
        });
        detError.show();
    }
}
