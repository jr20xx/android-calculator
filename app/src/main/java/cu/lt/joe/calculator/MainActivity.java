package cu.lt.joe.calculator;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.ActionMode;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.appcompat.widget.PopupMenu;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.snackbar.Snackbar;
import java.io.PrintWriter;
import java.io.StringWriter;
import cu.lt.joe.calculator.adapters.OperationsHistoryAdapter;
import cu.lt.joe.calculator.db.HistoryDatabaseHandler;
import cu.lt.joe.calculator.models.HistoryItem;
import cu.lt.joe.jcalc.JCalc;
import cu.lt.joe.jcalc.exceptions.InfiniteResultException;
import cu.lt.joe.jcalc.exceptions.NotNumericResultException;
import cu.lt.joe.jcalc.exceptions.UnbalancedParenthesesException;

public class MainActivity extends AppCompatActivity implements View.OnClickListener, View.OnLongClickListener
{
    private EditText screen;
    private boolean solved;
    private HistoryDatabaseHandler operations_records_handler;
    private ListView history_lv;
    private DrawerLayout history_drawer;
    private SharedPreferences sharp;
    private SharedPreferences.Editor editor;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setRequestedOrientation(android.content.pm.ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        sharp = PreferenceManager.getDefaultSharedPreferences(MainActivity.this);
        editor = sharp.edit();
        AppCompatDelegate.setDefaultNightMode(sharp.getBoolean("UI_MODE_DARK", false) ? AppCompatDelegate.MODE_NIGHT_NO : AppCompatDelegate.MODE_NIGHT_YES);
        setContentView(R.layout.main);
        history_drawer = findViewById(R.id.history_drawer);
        history_lv = findViewById(R.id.history_lv);
        history_lv.setDividerHeight(0);
        screen = findViewById(R.id.screen);
        screen.setShowSoftInputOnFocus(false);
        screen.setCursorVisible(false);
        screen.setText(getIntent().getStringExtra("screen_content"));
        solved = getIntent().getBooleanExtra("solved", false);
        operations_records_handler = new HistoryDatabaseHandler(this, () ->
        {
            history_lv.setAdapter(new OperationsHistoryAdapter(MainActivity.this, operations_records_handler.getOperationsHistory()));
            solved = true;
        });
        history_lv.setAdapter(new OperationsHistoryAdapter(MainActivity.this, operations_records_handler.getOperationsHistory()));
        screen.setCustomSelectionActionModeCallback(new ActionMode.Callback()
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

        screen.setOnKeyListener((v, keyCode, event) ->
        {
            if (event.getKeyCode() == KeyEvent.KEYCODE_BACK)
                return false;
            return true;
        });

        history_lv.setOnItemClickListener((p1, p2, p3, p4) ->
        {
            history_drawer.closeDrawer(GravityCompat.END);
            solved = false;
            screen.setText(((HistoryItem) p1.getItemAtPosition(p3)).getResult());
        });
        history_lv.setOnItemLongClickListener((p1, p2, p3, p4) ->
        {
            showHistoryItemPopupMenu((HistoryItem) p1.getItemAtPosition(p3), p2);
            return true;
        });
        ((androidx.appcompat.widget.AppCompatImageButton) findViewById(R.id.toggle_dayNight_mode)).setImageResource(sharp.getBoolean("UI_MODE_DARK", false) ? R.drawable.ic_enable_dark_mode : R.drawable.ic_enable_light_mode);
        findViewById(R.id.delete).setOnLongClickListener(this);
        screen.setOnLongClickListener(this);
    }

    @Override
    public void onClick(View p1)
    {
        String text = screen.getText().toString();
        char lastChar = !text.isEmpty() ? text.charAt(text.length() - 1) : ' ';
        switch (p1.getId())
        {
            case R.id.zero:
            case R.id.one:
            case R.id.two:
            case R.id.three:
            case R.id.four:
            case R.id.five:
            case R.id.six:
            case R.id.seven:
            case R.id.eight:
            case R.id.nine:
            {
                String number = ((Button) p1).getText().toString();
                if (!text.isEmpty())
                {
                    if (solved)
                    {
                        solved = false;
                        if (lastChar == '(' || lastChar == '.')
                            screen.setText(String.format("%s%s", text, number));
                        else
                            screen.setText(number);
                    }
                    else if (lastChar == ')')
                        screen.setText(String.format("%s×%s", text, number));
                    else
                        screen.setText(String.format("%s%s", text, number));
                }
                else
                    screen.setText(number);
                screen.setSelection(screen.getText().length());
                break;
            }
            case R.id.add:
            case R.id.subtract:
            case R.id.multiply:
            case R.id.divide:
            case R.id.exponentiation:
            {
                String operator = ((Button) p1).getText().toString();
                solved = false;
                if (!text.isEmpty())
                {
                    if (isNumber(lastChar + "") || lastChar == ')')
                        text += operator;
                    else if (lastChar == '.')
                        text += "0" + operator;
                    else if (lastChar == '(' && operator.equals("-"))
                        text += operator;
                    else if (text.endsWith("(-"))
                        text = operator.equals("+") ? text.substring(0, text.length() - 1) : text;
                    else if (isOperator(lastChar + ""))
                        text = text.substring(0, text.length() - 1) + operator;
                    screen.setText(text);
                    screen.setSelection(screen.getText().length());
                }
                break;
            }
            case R.id.comma:
            {
                if (!text.isEmpty())
                {
                    if (isNumber(lastChar + ""))
                    {
                        String arr[] = text.replace("(", "( ").replace("+", " + ").replace("-", " - ").replace("×", " × ").replace("÷", " ÷ ").replace("%", " % ").replace(")", " )").replace("n", "-").trim().split(" ");
                        if (!arr[arr.length - 1].contains("."))
                            screen.setText(String.format("%s.", screen.getText()));
                    }
                    else if (isOperator(lastChar + "") || lastChar == '(')
                        screen.setText(String.format("%s0.", screen.getText()));
                    screen.setSelection(screen.getText().length());
                }
                break;
            }
            case R.id.delete:
            {
                if (solved)
                {
                    solved = false;
                    screen.setText("");
                }
                else if (!text.isEmpty())
                {
                    text = text.substring(0, text.length() - 1);
                    screen.setText(text);
                    screen.setSelection(text.length());
                }
                break;
            }
            case R.id.openParentheses:
            {
                if (!text.isEmpty() && (isNumber(lastChar + "") || lastChar == ')'))
                {
                    screen.setText(String.format("%s×(", text));
                    screen.setSelection(screen.getText().length());
                }
                else
                {
                    screen.setText(String.format("%s(", text));
                    screen.setSelection(screen.getText().length());
                }
                break;
            }
            case R.id.closeParentheses:
            {
                if (text.contains("("))
                {
                    screen.setText(text += lastChar == '(' ? "1)" : ")");
                    screen.setSelection(text.length());
                }
                break;
            }
            case R.id.equal:
            {
                if (!text.isEmpty())
                {
                    try
                    {
                        if (text.contains("+") || text.contains("-") || text.contains("×") || text.contains("÷") || text.contains("^"))
                        {
                            String result = JCalc.performMathOperation(text);
                            screen.setText(result);
                            screen.setSelection(0);
                            operations_records_handler.saveOperation(text, result);
                        }
                    }
                    catch (NotNumericResultException e)
                    {
                        screen.setText("");
                        Snackbar.make(screen, R.string.mistcalc, Snackbar.LENGTH_LONG).setAction(R.string.more_details, v -> showDetails(MainActivity.this, "NULO")).show();
                    }
                    catch (InfiniteResultException e)
                    {
                        screen.setText("");
                        Snackbar.make(screen, R.string.mistcalc, Snackbar.LENGTH_LONG).setAction(R.string.more_details, v -> showDetails(MainActivity.this, "INF")).show();
                    }
                    catch (UnbalancedParenthesesException x)
                    {
                        screen.setText("");
                        Snackbar.make(screen, R.string.mistcalc, Snackbar.LENGTH_LONG).setAction(R.string.more_details, v -> showDetails(MainActivity.this, "PIE")).show();
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
                        Snackbar.make(screen, R.string.mistcalc, Snackbar.LENGTH_LONG).setAction(R.string.more_details, v -> showDetails(MainActivity.this, exception)).show();
                    }
                }
                break;
            }
            case R.id.toggle_history_drawer:
            {
                history_drawer.openDrawer(GravityCompat.END);
                break;
            }
            case R.id.clearHistory_button:
            {
                operations_records_handler.clearRecords();
                break;
            }
            case R.id.toggle_dayNight_mode:
            {
                editor.putBoolean("UI_MODE_DARK", !(sharp.getBoolean("UI_MODE_DARK", false))).commit();
                startActivity(new Intent(getApplicationContext(), MainActivity.class).setFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION).setFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK).setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP).putExtra("solved", solved).putExtra("screen_content", screen.getText().toString()));
                overridePendingTransition(R.anim.fade_in, R.anim.fade_out);
                break;
            }
        }
    }

    @Override
    public boolean onLongClick(View p1)
    {
        switch (p1.getId())
        {
            case R.id.delete:
            {
                screen.setText("");
                return true;
            }
            case R.id.screen:
            {
                if (!screen.getText().toString().isEmpty())
                {
                    final BottomSheetDialog dialog = new BottomSheetDialog(MainActivity.this);
                    View edits = getLayoutInflater().inflate(R.layout.screen_menu, null);
                    dialog.setContentView(edits);
                    edits.findViewById(R.id.copy_lay).setOnClickListener(view ->
                    {
                        copyToClipboard(null, screen.getText().toString());
                        dialog.dismiss();
                        Snackbar.make(screen, R.string.copied, Snackbar.LENGTH_SHORT).show();
                    });
                    edits.findViewById(R.id.cut_lay).setOnClickListener(view ->
                    {
                        solved = false;
                        copyToClipboard(null, screen.getText().toString());
                        dialog.dismiss();
                        screen.setText("");
                        Snackbar.make(screen, R.string.cutText, Snackbar.LENGTH_SHORT).show();
                    });
                    dialog.show();
                }
                return true;
            }
            default:
                return false;
        }
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
            String number = Integer.toString(i);
            if (number.equals(x))
                return true;
        }
        return false;
    }

    @NonNull
    private Boolean isOperator(String x)
    {
        return (x.equals("+")) || (x.equals("-")) || (x.equals("×")) || (x.equals("÷")) || (x.equals("^"));
    }

    @Override
    public void onBackPressed()
    {
        final BottomSheetDialog exit = new BottomSheetDialog(this);
        View inflado = getLayoutInflater().inflate(R.layout.exit, null);
        exit.setContentView(inflado);
        exit.setCancelable(false);
        inflado.findViewById(R.id.I_do_want).setOnClickListener(v ->
        {
            exit.dismiss();
            finishAffinity();
            overridePendingTransition(R.anim.fade_in, R.anim.fade_out);
            android.os.Process.killProcess(android.os.Process.myPid());
            startActivity(new Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME));
        });
        inflado.findViewById(R.id.I_dont_want).setOnClickListener(v -> exit.dismiss());
        exit.show();
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