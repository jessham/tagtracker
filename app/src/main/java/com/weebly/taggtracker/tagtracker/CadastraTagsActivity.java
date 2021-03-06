package com.weebly.taggtracker.tagtracker;

import android.app.Activity;
import android.app.PendingIntent;
import android.content.Intent;
import android.nfc.NfcAdapter;
import android.nfc.Tag;
import android.nfc.tech.MifareUltralight;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import android.app.AlertDialog;
import android.content.DialogInterface;

import java.io.IOException;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;

public class CadastraTagsActivity extends AppCompatActivity {
    private DatabaseHelper bd;
    private Toolbar toolbar;
    private NfcAdapter mNfcAdapter;
    private PendingIntent pendingIntent;
    private Tag mytag;
    private String rotulo;
    private String rotuloOriginal;
    private boolean modoEditar;
    private boolean clicouSalvar;


    /* ********************************************************************************************
        GETTERS AND SETTERS
     ******************************************************************************************** */


    public String getRotulo() {
        return rotulo;
    }

    public void setRotulo(String rotulo) {
        this.rotulo = rotulo;
    }

    public boolean isModoEditar() {
        return modoEditar;
    }

    public void setModoEditar(boolean modoEditar) {
        this.modoEditar = modoEditar;
    }

    public CadastraTagsActivity() {
        bd = new DatabaseHelper(this);
    }

    public Toolbar getToolbar() {
        return toolbar;
    }

    public void setToolbar(Toolbar toolbar) {
        this.toolbar = toolbar;
    }

    /* ********************************************************************************************
        MÉTODOS DE INICIALIZAÇÃO
     ******************************************************************************************** */


    @Override
    protected void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_cadastra_tags);

        //Configura a toolbar
        toolbar = (Toolbar) findViewById(R.id.toolbar_cadastraTag);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        rotulo = new String("");

        // Habilitando o NFC
        mNfcAdapter = NfcAdapter.getDefaultAdapter(CadastraTagsActivity.this);
        pendingIntent = PendingIntent.getActivity(CadastraTagsActivity.this, 0, new Intent(CadastraTagsActivity.this,
                getClass()).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP), 0);


        //Verifica se esta no modulo de edicao
        Bundle extras = getIntent().getExtras();
        if (extras != null) {
            setRotulo(extras.getString("tag-edit-key"));
            setModoEditar(true);

            EditText txtTitulo = (EditText) findViewById(R.id.txtTitleTag);
            txtTitulo.setText(getRotulo());
            rotuloOriginal = getRotulo();
        }

        // configurar o subtítulo
        TextView subtitle = (TextView) findViewById(R.id.txtAddTag);
        if (isModoEditar()) {
            subtitle.setText("Editar tag");
            toolbar.setTitle(R.string.title_VisualizaTagsActivity);
        } else {
            subtitle.setText("Adicionar tag");
            toolbar.setTitle(R.string.title_CadastraTagsActivity);
        }

        //Comportamento para salvar a tag
        View btnSalvar = findViewById(R.id.btnSalvarTag);
        btnSalvar.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                clicouSalvar = true;
                salvaTag();
            }
        });

        //Comportamento para cancelar salvar a tag
        View btnCancelar = findViewById(R.id.btnCancelartag);
        btnCancelar.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });
    }

    /*@Override
    public void onBackPressed() {
        // 1. Instantiate an AlertDialog.Builder with its constructor
        AlertDialog.Builder builder = new AlertDialog.Builder(CadastraTagsActivity.this);
        // 2. Chain together various setter methods to set the dialog characteristics
        builder.setMessage("O item não será salvo sem ter uma tag NFC atribuída à ela")
                .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        finish();
                    }
                })
                .setNegativeButton("Cancelar", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        // User cancelled the dialog
                        // 1. Instantiate an AlertDialog.Builder with its constructor
                        AlertDialog.Builder builder = new AlertDialog.Builder(CadastraTagsActivity.this);
                        // 2. Chain together various setter methods to set the dialog characteristics
                        builder.setMessage("Aproxime uma tag NFC para completar o cadastro da tag").setTitle("");
                        // 3. Get the AlertDialog from create()
                        AlertDialog secondChance = builder.create();
                        secondChance.show();
                    }
                });
        // 3. Get the AlertDialog from create()
        AlertDialog dialog = builder.create();
        dialog.show();
    }*/

    @Override
    protected void onResume() {
        super.onResume();

        /**
         * It's important, that the activity is in the foreground (resumed). Otherwise
         * an IllegalStateException is thrown.
         */

        if (mNfcAdapter != null) {
            mNfcAdapter.enableForegroundDispatch(this, pendingIntent, null, null);
        }
    }

    @Override
    protected void onPause() {
        /**
         * Call this before onPause, otherwise an IllegalArgumentException is thrown as well.
         */

        if (mNfcAdapter != null) {
            mNfcAdapter.disableForegroundDispatch(this);
        }

        super.onPause();
    }

    @Override
    protected void onNewIntent(Intent intent) {
        if (mNfcAdapter != null) {
            if (NfcAdapter.ACTION_TAG_DISCOVERED.equals(intent.getAction())) {
                mytag = intent.getParcelableExtra(NfcAdapter.EXTRA_TAG);
                if (clicouSalvar) {
                    if (!isModoEditar()) {
                        writeTag(mytag, bd.retornaProxIDTags());
                        if (bd.insereTags(rotulo)) {
                            Toast.makeText(this, "A tag foi detectada e salva", Toast.LENGTH_LONG).show();
                            setResult(Activity.RESULT_OK, getIntent());
                        }
                    } else {
                        int editedTagId = bd.buscaIdTag(rotuloOriginal);
                        writeTag(mytag, editedTagId);
                        if (bd.atualizatags(rotulo, editedTagId)) {
                            Toast.makeText(this, "A tag foi editada", Toast.LENGTH_LONG).show();
                            setResult(Activity.RESULT_OK, getIntent());
                        }
                    }
                    finish();
                }
            }
        }
    }


    /* ********************************************************************************************
        OUTROS
     ******************************************************************************************** */

    public void writeTag(Tag tag, int tagId) {
        MifareUltralight ultralight = MifareUltralight.get(tag);
        try {
            ultralight.connect();

            // transformar id do item em 4 bytes
            String tagText = Integer.toString(tagId);
            String idFormattedToString = String.format("%0" + (4 - tagText.length()) + "d%s", 0, tagText);

            ultralight.writePage(4, idFormattedToString.getBytes(Charset.forName("UTF-8")));

        } catch (IOException e) {
            //Log.e(TAG, "IOException while closing MifareUltralight...", e);
            Toast.makeText(this, "Ocorreu um problema ao salvar na tag", Toast.LENGTH_LONG).show();
            finish();
        } finally {
            try {
                ultralight.close();
            } catch (IOException e) {
                //Log.e(TAG, "IOException while closing MifareUltralight...", e);
                Toast.makeText(this, "Ocorreu um problema ao salvar na tag", Toast.LENGTH_LONG).show();
                finish();
            }
        }
    }


    public void salvaTag() {
        final EditText txtTitulo = (EditText) findViewById(R.id.txtTitleTag);

        //Verifica se vazio
        if (txtTitulo.getText().toString().isEmpty()) {
            txtTitulo.setError("Digite um rótulo!");
            txtTitulo.setText("");
            return;
        }
        //Verifica se maior que 3  30 caracteres
        if (txtTitulo.getText().length() < 3 || txtTitulo.getText().length() > 30) {
            txtTitulo.setError("O rótulo deve ter entre 3 e 30 caracteres!");
            txtTitulo.setText("");
            return;
        }

        if (!isModoEditar()) {
            //Verifica se existe tag igual
            ArrayList<String> listaTotal = bd.leTags();

            for (int i = 0; i < listaTotal.size(); i++) {
                if (listaTotal.get(i).toLowerCase().contains(txtTitulo.getText().toString().toLowerCase())) {
                    txtTitulo.setError("Já existe uma tag com esse rotulo!");
                    txtTitulo.setText("");
                    return;
                }
            }
        }

        rotulo = txtTitulo.getText().toString();
        // 1. Instantiate an AlertDialog.Builder with its constructor
        AlertDialog.Builder builder = new AlertDialog.Builder(CadastraTagsActivity.this);
        // 2. Chain together various setter methods to set the dialog characteristics
        builder.setMessage("Aproxime uma tag NFC para completar o cadastro").setTitle("");
        // 3. Get the AlertDialog from create()
        AlertDialog dialog = builder.create();
        dialog.show();

    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {


        if (isModoEditar()) {
            new MenuInflater(this).inflate(R.menu.menu_toolbar, menu);
            apareceMenu(false, true, false);
            toolbar.getMenu().findItem(R.id.app_bar_delete).setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);
        } else
            return true;

        return (super.onCreateOptionsMenu(menu));
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        switch (item.getItemId()) {


            case R.id.app_bar_delete:
                deleta();
                return true;

            default:
                // If we got here, the user's action was not recognized.
                // Invoke the superclass to handle it.
                return super.onOptionsItemSelected(item);

        }
    }

    //Configura as opções visiveis do menu da toolbar
    public void apareceMenu(boolean pesquisa, boolean deleta, boolean edita) {
        getToolbar().getMenu().findItem(R.id.app_bar_delete).setVisible(deleta);
        getToolbar().getMenu().findItem(R.id.app_bar_search).setVisible(pesquisa);
        getToolbar().getMenu().findItem(R.id.app_bar_edit).setVisible(edita);
    }


    public boolean deleta() {
        //Toast.makeText(getApplicationContext(), "Pegando os ids", Toast.LENGTH_SHORT).show();
        final int aDeletarIds = bd.buscaIdTag(getRotulo());


        //procura saber se tag eh a ultima da checklist
        final List<Integer> resp = bd.buscaAssocia(String.valueOf(aDeletarIds));

        if (resp != null) {
            if (resp.size() > 0) {

                android.support.v7.app.AlertDialog.Builder builder = new android.support.v7.app.AlertDialog.Builder(this);
                builder.setTitle(R.string.delete_title);
                builder.setMessage(R.string.msg_deleteAssocia);


                builder.setPositiveButton(R.string.btnYes, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {

                        for (int k = 0; k < resp.size(); k++) {
                            bd.deletaAssocia(resp.get(k), -1);
                            bd.deletaChecklists(resp.get(k));
                        }


                        if (bd.deletaTags(aDeletarIds)) {
                            Toast.makeText(getApplicationContext(),R.string.tag_deleted,Toast.LENGTH_SHORT).show();
                            setResult(Activity.RESULT_OK, getIntent());
                            finish();
                        }

                        dialog.dismiss();
                    }

                });

                builder.setNegativeButton(R.string.btnNo, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                        return;
                    }
                });

                android.support.v7.app.AlertDialog alert = builder.create();
                alert.show();
            } else {
                if (bd.deletaTags(aDeletarIds))
                    setResult(Activity.RESULT_OK, getIntent());
                finish();
            }

        }
        return true;
    }
}







