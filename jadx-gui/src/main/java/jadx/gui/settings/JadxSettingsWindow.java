package jadx.gui.settings;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ItemEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.Arrays;
import java.util.Collection;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import say.swing.JFontChooser;

import jadx.gui.ui.MainWindow;
import jadx.gui.ui.codearea.EditorTheme;
import jadx.gui.utils.LangLocale;
import jadx.gui.utils.NLS;

public class JadxSettingsWindow extends JDialog {
	private static final long serialVersionUID = -1804570470377354148L;

	private static final Logger LOG = LoggerFactory.getLogger(JadxSettingsWindow.class);

	private final transient MainWindow mainWindow;
	private final transient JadxSettings settings;
	private final transient String startSettings;
	private final transient LangLocale prevLang;

	private transient boolean needReload = false;

	public JadxSettingsWindow(MainWindow mainWindow, JadxSettings settings) {
		this.mainWindow = mainWindow;
		this.settings = settings;
		this.startSettings = JadxSettingsAdapter.makeString(settings);
		this.prevLang = settings.getLangLocale();

		initUI();

		setTitle(NLS.str("preferences.title"));
		setSize(400, 550);
		setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
		setModalityType(ModalityType.APPLICATION_MODAL);
		pack();
		setLocationRelativeTo(null);
	}

	private void initUI() {
		JPanel panel = new JPanel();
		panel.setLayout(new BoxLayout(panel, BoxLayout.PAGE_AXIS));
		panel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
		panel.add(makeDeobfuscationGroup());
		panel.add(makeDecompilationGroup());
		panel.add(makeEditorGroup());
		panel.add(makeOtherGroup());

		JButton saveBtn = new JButton(NLS.str("preferences.save"));
		saveBtn.addActionListener(event -> {
			settings.sync();
			if (needReload) {
				mainWindow.reOpenFile();
			}
			if (!settings.getLangLocale().equals(prevLang)) {
				JOptionPane.showMessageDialog(
						this,
						NLS.str("msg.language_changed", settings.getLangLocale()),
						NLS.str("msg.language_changed_title", settings.getLangLocale()),
						JOptionPane.INFORMATION_MESSAGE
				);
			}
			dispose();
		});
		JButton cancelButton = new JButton(NLS.str("preferences.cancel"));
		cancelButton.addActionListener(event -> {
			JadxSettingsAdapter.fill(settings, startSettings);
			dispose();
		});

		JButton resetBtn = new JButton(NLS.str("preferences.reset"));
		resetBtn.addActionListener(event -> {
			int res = JOptionPane.showConfirmDialog(
					JadxSettingsWindow.this,
					NLS.str("preferences.reset_message"),
					NLS.str("preferences.reset_title"),
					JOptionPane.YES_NO_OPTION);
			if (res == JOptionPane.YES_OPTION) {
				String defaults = JadxSettingsAdapter.makeString(JadxSettings.makeDefault());
				JadxSettingsAdapter.fill(settings, defaults);
				getContentPane().removeAll();
				initUI();
				pack();
				repaint();
			}
		});

		JPanel buttonPane = new JPanel();
		buttonPane.setLayout(new BoxLayout(buttonPane, BoxLayout.LINE_AXIS));
		buttonPane.setBorder(BorderFactory.createEmptyBorder(0, 10, 10, 10));
		buttonPane.add(resetBtn);
		buttonPane.add(Box.createHorizontalGlue());
		buttonPane.add(saveBtn);
		buttonPane.add(Box.createRigidArea(new Dimension(10, 0)));
		buttonPane.add(cancelButton);

		Container contentPane = getContentPane();
		contentPane.add(panel, BorderLayout.CENTER);
		contentPane.add(buttonPane, BorderLayout.PAGE_END);
		getRootPane().setDefaultButton(saveBtn);
	}

	private SettingsGroup makeDeobfuscationGroup() {
		JCheckBox deobfOn = new JCheckBox();
		deobfOn.setSelected(settings.isDeobfuscationOn());
		deobfOn.addItemListener(e -> {
			settings.setDeobfuscationOn(e.getStateChange() == ItemEvent.SELECTED);
			needReload();
		});

		JCheckBox deobfForce = new JCheckBox();
		deobfForce.setSelected(settings.isDeobfuscationForceSave());
		deobfForce.addItemListener(e -> {
			settings.setDeobfuscationForceSave(e.getStateChange() == ItemEvent.SELECTED);
			needReload();
		});

		SpinnerNumberModel minLenModel = new SpinnerNumberModel(settings.getDeobfuscationMinLength(), 0, Integer.MAX_VALUE, 1);
		JSpinner minLenSpinner = new JSpinner(minLenModel);
		minLenSpinner.addChangeListener(e -> {
			settings.setDeobfuscationMinLength((Integer) minLenSpinner.getValue());
			needReload();
		});

		SpinnerNumberModel maxLenModel = new SpinnerNumberModel(settings.getDeobfuscationMaxLength(), 0, Integer.MAX_VALUE, 1);
		JSpinner maxLenSpinner = new JSpinner(maxLenModel);
		maxLenSpinner.addChangeListener(e -> {
			settings.setDeobfuscationMaxLength((Integer) maxLenSpinner.getValue());
			needReload();
		});

		JCheckBox deobfSourceAlias = new JCheckBox();
		deobfSourceAlias.setSelected(settings.isDeobfuscationUseSourceNameAsAlias());
		deobfSourceAlias.addItemListener(e -> {
			settings.setDeobfuscationUseSourceNameAsAlias(e.getStateChange() == ItemEvent.SELECTED);
			needReload();
		});

		SettingsGroup deobfGroup = new SettingsGroup(NLS.str("preferences.deobfuscation"));
		deobfGroup.addRow(NLS.str("preferences.deobfuscation_on"), deobfOn);
		deobfGroup.addRow(NLS.str("preferences.deobfuscation_force"), deobfForce);
		deobfGroup.addRow(NLS.str("preferences.deobfuscation_min_len"), minLenSpinner);
		deobfGroup.addRow(NLS.str("preferences.deobfuscation_max_len"), maxLenSpinner);
		deobfGroup.addRow(NLS.str("preferences.deobfuscation_source_alias"), deobfSourceAlias);
		deobfGroup.end();

		Collection<JComponent> connectedComponents = Arrays.asList(deobfForce, minLenSpinner, maxLenSpinner, deobfSourceAlias);
		deobfOn.addItemListener(e -> enableComponentList(connectedComponents, e.getStateChange() == ItemEvent.SELECTED));
		enableComponentList(connectedComponents, settings.isDeobfuscationOn());
		return deobfGroup;
	}

	private void enableComponentList(Collection<JComponent> connectedComponents, boolean enabled) {
		connectedComponents.forEach(comp -> comp.setEnabled(enabled));
	}

	private SettingsGroup makeEditorGroup() {
		JButton fontBtn = new JButton(NLS.str("preferences.select_font"));
		fontBtn.addMouseListener(new MouseAdapter() {
			@Override
			public void mouseClicked(MouseEvent e) {
				JFontChooser fontChooser = new JFontChooser();
				fontChooser.setSelectedFont(settings.getFont());
				int result = fontChooser.showDialog(JadxSettingsWindow.this);
				if (result == JFontChooser.OK_OPTION) {
					Font font = fontChooser.getSelectedFont();
					LOG.debug("Selected Font: {}", font);
					settings.setFont(font);
					mainWindow.updateFont(font);
					mainWindow.loadSettings();
				}
			}
		});

		EditorTheme[] editorThemes = EditorTheme.getAllThemes();
		JComboBox<EditorTheme> themesCbx = new JComboBox<>(editorThemes);
		for (EditorTheme theme : editorThemes) {
			if (theme.getPath().equals(settings.getEditorThemePath())) {
				themesCbx.setSelectedItem(theme);
				break;
			}
		}
		themesCbx.addActionListener(e -> {
			int i = themesCbx.getSelectedIndex();
			EditorTheme editorTheme = editorThemes[i];
			settings.setEditorThemePath(editorTheme.getPath());
			mainWindow.loadSettings();
		});

		SettingsGroup other = new SettingsGroup(NLS.str("preferences.editor"));
		other.addRow(NLS.str("preferences.font"), fontBtn);
		other.addRow(NLS.str("preferences.theme"), themesCbx);
		return other;
	}

	private SettingsGroup makeDecompilationGroup() {
		JCheckBox fallback = new JCheckBox();
		fallback.setSelected(settings.isFallbackMode());
		fallback.addItemListener(e -> {
			settings.setFallbackMode(e.getStateChange() == ItemEvent.SELECTED);
			needReload();
		});

		JCheckBox showInconsistentCode = new JCheckBox();
		showInconsistentCode.setSelected(settings.isShowInconsistentCode());
		showInconsistentCode.addItemListener(e -> {
			settings.setShowInconsistentCode(e.getStateChange() == ItemEvent.SELECTED);
			needReload();
		});

		JCheckBox resourceDecode = new JCheckBox();
		resourceDecode.setSelected(settings.isSkipResources());
		resourceDecode.addItemListener(e -> {
			settings.setSkipResources(e.getStateChange() == ItemEvent.SELECTED);
			needReload();
		});

		SpinnerNumberModel spinnerModel = new SpinnerNumberModel(
				settings.getThreadsCount(), 1, Runtime.getRuntime().availableProcessors() * 2, 1);
		JSpinner threadsCount = new JSpinner(spinnerModel);
		threadsCount.addChangeListener(e -> {
			settings.setThreadsCount((Integer) threadsCount.getValue());
			needReload();
		});

		JButton editExcludedPackages = new JButton(NLS.str("preferences.excludedPackages.button"));
		editExcludedPackages.addActionListener(event -> {

			String result = JOptionPane.showInputDialog(this, NLS.str("preferences.excludedPackages.editDialog"),
					settings.getExcludedPackages());
			if (result != null) {
				settings.setExcludedPackages(result);
			}
		});

		JCheckBox autoStartJobs = new JCheckBox();
		autoStartJobs.setSelected(settings.isAutoStartJobs());
		autoStartJobs.addItemListener(e -> settings.setAutoStartJobs(e.getStateChange() == ItemEvent.SELECTED));

		JCheckBox escapeUnicode = new JCheckBox();
		escapeUnicode.setSelected(settings.escapeUnicode());
		escapeUnicode.addItemListener(e -> {
			settings.setEscapeUnicode(e.getStateChange() == ItemEvent.SELECTED);
			needReload();
		});

		JCheckBox replaceConsts = new JCheckBox();
		replaceConsts.setSelected(settings.isReplaceConsts());
		replaceConsts.addItemListener(e -> {
			settings.setReplaceConsts(e.getStateChange() == ItemEvent.SELECTED);
			needReload();
		});

		JCheckBox useImports = new JCheckBox();
		useImports.setSelected(settings.isUseImports());
		useImports.addItemListener(e -> {
			settings.setUseImports(e.getStateChange() == ItemEvent.SELECTED);
			needReload();
		});

		SettingsGroup other = new SettingsGroup(NLS.str("preferences.decompile"));
		other.addRow(NLS.str("preferences.threads"), threadsCount);
		other.addRow(NLS.str("preferences.excludedPackages"), NLS.str("preferences.excludedPackages.tooltip"),
				editExcludedPackages);
		other.addRow(NLS.str("preferences.start_jobs"), autoStartJobs);
		other.addRow(NLS.str("preferences.showInconsistentCode"), showInconsistentCode);
		other.addRow(NLS.str("preferences.escapeUnicode"), escapeUnicode);
		other.addRow(NLS.str("preferences.replaceConsts"), replaceConsts);
		other.addRow(NLS.str("preferences.useImports"), useImports);
		other.addRow(NLS.str("preferences.fallback"), fallback);
		other.addRow(NLS.str("preferences.skipResourcesDecode"), resourceDecode);
		return other;
	}

	private SettingsGroup makeOtherGroup() {
		JComboBox<LangLocale> languageCbx = new JComboBox<>(NLS.getI18nLocales());
		for (LangLocale locale : NLS.getI18nLocales()) {
			if (locale.equals(settings.getLangLocale())) {
				languageCbx.setSelectedItem(locale);
				break;
			}
		}
		languageCbx.addActionListener(e -> settings.setLangLocale((LangLocale) languageCbx.getSelectedItem()));

		JCheckBox update = new JCheckBox();
		update.setSelected(settings.isCheckForUpdates());
		update.addItemListener(e -> settings.setCheckForUpdates(e.getStateChange() == ItemEvent.SELECTED));

		JCheckBox cfg = new JCheckBox();
		cfg.setSelected(settings.isCfgOutput());
		cfg.addItemListener(e -> {
			settings.setCfgOutput(e.getStateChange() == ItemEvent.SELECTED);
			needReload();
		});

		JCheckBox rawCfg = new JCheckBox();
		rawCfg.setSelected(settings.isRawCfgOutput());
		rawCfg.addItemListener(e -> {
			settings.setRawCfgOutput(e.getStateChange() == ItemEvent.SELECTED);
			needReload();
		});

		SettingsGroup other = new SettingsGroup(NLS.str("preferences.other"));
		other.addRow(NLS.str("preferences.language"), languageCbx);
		other.addRow(NLS.str("preferences.check_for_updates"), update);
		other.addRow(NLS.str("preferences.cfg"), cfg);
		other.addRow(NLS.str("preferences.raw_cfg"), rawCfg);
		return other;
	}

	private void needReload() {
		needReload = true;
	}

	private static class SettingsGroup extends JPanel {
		private static final long serialVersionUID = -6487309975896192544L;

		private final GridBagConstraints c;
		private int row;

		public SettingsGroup(String title) {
			setBorder(BorderFactory.createTitledBorder(title));
			setLayout(new GridBagLayout());
			c = new GridBagConstraints();
			c.insets = new Insets(5, 5, 5, 5);
			c.weighty = 1.0;
		}

		public void addRow(String label, JComponent comp) {
			addRow(label, null, comp);
		}

		public void addRow(String label, String tooltip, JComponent comp) {
			c.gridy = row++;
			JLabel jLabel = new JLabel(label);
			jLabel.setLabelFor(comp);
			jLabel.setHorizontalAlignment(SwingConstants.LEFT);
			c.gridx = 0;
			c.gridwidth = 1;
			c.anchor = GridBagConstraints.LINE_START;
			c.weightx = 0.8;
			c.fill = GridBagConstraints.NONE;
			add(jLabel, c);
			c.gridx = 1;
			c.gridwidth = GridBagConstraints.REMAINDER;
			c.anchor = GridBagConstraints.CENTER;
			c.weightx = 0.2;
			c.fill = GridBagConstraints.HORIZONTAL;

			if (tooltip != null) {
				jLabel.setToolTipText(tooltip);
				comp.setToolTipText(tooltip);
			}

			add(comp, c);

			comp.addPropertyChangeListener("enabled", evt -> jLabel.setEnabled((boolean) evt.getNewValue()));
		}

		public void end() {
			add(Box.createVerticalGlue());
		}
	}
}
