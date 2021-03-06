package com.harrysoft.burstcoinexplorer.explore.ui.browse;

import android.arch.lifecycle.ViewModelProviders;
import android.nfc.NfcAdapter;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.harrysoft.burstcoinexplorer.R;
import com.harrysoft.burstcoinexplorer.burst.entity.AccountWithRewardRecipient;
import com.harrysoft.burstcoinexplorer.burst.entity.BlockWithGenerator;
import com.harrysoft.burstcoinexplorer.explore.viewmodel.browse.ViewBlockDetailsViewModel;
import com.harrysoft.burstcoinexplorer.explore.viewmodel.browse.ViewBlockDetailsViewModelFactory;
import com.harrysoft.burstcoinexplorer.main.repository.ClipboardRepository;
import com.harrysoft.burstcoinexplorer.main.router.ExplorerRouter;
import com.harrysoft.burstcoinexplorer.util.FileSizeUtils;
import com.harrysoft.burstcoinexplorer.util.TextFormatUtils;
import com.harrysoft.burstcoinexplorer.util.TextViewUtils;
import com.harrysoft.burstcoinexplorer.util.TimestampUtils;

import java.math.BigInteger;
import java.util.Locale;
import java.util.Objects;

import javax.inject.Inject;

import burst.kit.entity.response.BlockResponse;
import dagger.android.AndroidInjection;

public class ViewBlockDetailsActivity extends ViewDetailsActivity {

    @Inject
    ClipboardRepository clipboardRepository;
    @Inject
    ViewBlockDetailsViewModelFactory viewBlockDetailsViewModelFactory;

    private TextView blockNumberText, blockIDText, timestampText, txCountText, totalText, sizeText, generatorText, rewardRecipientText, feeText;
    private Button viewExtraButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        AndroidInjection.inject(this);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_view_block_details);

        // Check for Block ID
        if (getIntent().getExtras() != null && getIntent().getExtras().containsKey(getString(R.string.extra_block_id))) {
            viewBlockDetailsViewModelFactory.setBlockID(new BigInteger(getIntent().getExtras().getString(getString(R.string.extra_block_id))));
        }

        // Check for Block Number
        else if (getIntent().getExtras() != null && getIntent().getExtras().containsKey(getString(R.string.extra_block_number))) {
            viewBlockDetailsViewModelFactory.setBlockNumber(new BigInteger(getIntent().getExtras().getString(getString(R.string.extra_block_number))));
        }

        if (!viewBlockDetailsViewModelFactory.canCreate()) {
            Toast.makeText(this, R.string.loading_error, Toast.LENGTH_LONG).show();
            finish();
            return;
        }

        ViewBlockDetailsViewModel viewBlockDetailsViewModel = ViewModelProviders.of(this, viewBlockDetailsViewModelFactory).get(ViewBlockDetailsViewModel.class);

        blockNumberText = findViewById(R.id.view_block_details_block_number_value);
        blockIDText = findViewById(R.id.view_block_details_block_id_value);
        timestampText = findViewById(R.id.view_block_details_timestamp_value);
        txCountText = findViewById(R.id.view_block_details_transaction_count_value);
        totalText = findViewById(R.id.view_block_details_total_value);
        sizeText = findViewById(R.id.view_block_details_size_value);
        generatorText = findViewById(R.id.view_block_details_generator_value);
        rewardRecipientText = findViewById(R.id.view_block_details_reward_recipient_value);
        feeText = findViewById(R.id.view_block_details_fee_value);
        viewExtraButton = findViewById(R.id.view_block_details_view_extra);

        viewExtraButton.setOnClickListener(view -> Toast.makeText(this, R.string.loading, Toast.LENGTH_SHORT).show());

        viewBlockDetailsViewModel.getBlock().observe(this, this::onBlock);

        if (NfcAdapter.getDefaultAdapter(this) != null) {
            NfcAdapter.getDefaultAdapter(this).setNdefPushMessageCallback(viewBlockDetailsViewModel, this);
        }
    }

    private void onBlock(@Nullable BlockWithGenerator blockWithGenerator) {
        if (blockWithGenerator != null && blockWithGenerator.getBlock() != null && blockWithGenerator.getGenerator() != null) {
            BlockResponse block = blockWithGenerator.getBlock();
            AccountWithRewardRecipient generator = blockWithGenerator.getGenerator();
            blockNumberText.setText(String.format(Locale.getDefault(), "%d", block.getHeight()));
            blockIDText.setText(block.getBlock().getID());
            timestampText.setText(TimestampUtils.formatBurstTimestamp(block.getTimestamp()));
            txCountText.setText(String.format(Locale.getDefault(), "%d", block.getNumberOfTransactions()));
            totalText.setText(block.getTotalAmountNQT().toString());
            sizeText.setText(FileSizeUtils.formatBlockSize(block));
            generatorText.setText(getString(R.string.address_display_format, block.getGenerator().getFullAddress(), TextFormatUtils.checkIfSet(this, generator.getAccount().getName())));
            rewardRecipientText.setText(getString(R.string.address_display_format, generator.getRewardRecipient().getFullAddress(), TextFormatUtils.checkIfSet(this, generator.getRewardRecipientName())));
            feeText.setText(block.getTotalFeeNQT().toString());
            configureViews(blockWithGenerator);
        } else {
            blockNumberText.setText(R.string.loading_error);
            blockIDText.setText(R.string.loading_error);
            timestampText.setText(R.string.loading_error);
            txCountText.setText(R.string.loading_error);
            totalText.setText(R.string.loading_error);
            sizeText.setText(R.string.loading_error);
            generatorText.setText(R.string.loading_error);
            rewardRecipientText.setText(R.string.loading_error);
            feeText.setText(R.string.loading_error);
        }
    }

    private void configureViews(BlockWithGenerator blockWithGenerator) {
        BlockResponse block = blockWithGenerator.getBlock();
        AccountWithRewardRecipient generator = blockWithGenerator.getGenerator();
        if (generator != null) {
            TextViewUtils.setupTextViewAsHyperlink(generatorText, (view) -> ExplorerRouter.viewAccountDetails(this, block.getGenerator().getBurstID()));
            TextViewUtils.setupTextViewAsCopyable(clipboardRepository, generatorText, block.getGenerator().getFullAddress());
        }

        if (generator != null && generator.getRewardRecipient() != null && !Objects.equals(block.getGenerator().getID(), generator.getRewardRecipient().getID())) {
            TextViewUtils.setupTextViewAsHyperlink(rewardRecipientText, (view) -> ExplorerRouter.viewAccountDetails(this, generator.getRewardRecipient().getBurstID()));
            TextViewUtils.setupTextViewAsCopyable(clipboardRepository, rewardRecipientText, generator.getRewardRecipient().getFullAddress());
        }

        viewExtraButton.setOnClickListener(v -> ExplorerRouter.viewBlockExtraDetails(this, block.getBlock()));

        TextViewUtils.setupTextViewAsCopyable(clipboardRepository, blockNumberText, String.valueOf(block.getHeight()));
        TextViewUtils.setupTextViewAsCopyable(clipboardRepository, blockIDText, block.getBlock().getID());
    }
}
