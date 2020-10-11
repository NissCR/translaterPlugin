package niss;

import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.PlatformDataKeys;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.ui.Messages;
import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import org.jsoup.internal.StringUtil;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.text.MessageFormat;
import java.util.ResourceBundle;

/**
 * Действие перевода выбранного в редакторе текста
 *
 * @author Niss
 */
public class TranslateAction extends AnAction {
    private static final Logger LOGGER = Logger.getLogger(TranslateAction.class.getName());
    private static final String INFO = "{0} - {1}";

    @Override
    public void actionPerformed(@NotNull AnActionEvent anActionEvent) {
        final Editor editor = anActionEvent.getData(PlatformDataKeys.EDITOR);
        if (editor == null) {
            return; // Действие не из редактора
        }
        final String selectedText = editor.getSelectionModel().getSelectedText();

        try {
            if (selectedText != null) {
                String stringToTranslate = obtainSplitString(selectedText).toLowerCase();
                String stringTranslated = translateText(stringToTranslate);

                Messages.showMessageDialog(MessageFormat.format(INFO, stringToTranslate, stringTranslated),
                        "Translater", Messages.getInformationIcon());
            }
        } catch (Exception e) {
            LOGGER.error(e.getMessage());
        }
    }

    @Override
    public boolean isDumbAware() {
        return false;
    }

    /**
     * Разделяет выражение на слова по camelCase ('camelCase' -> 'camel Case')
     *
     * @param str строка в camelCase
     * @return слова, разделенные пробелом
     */
    private String obtainSplitString(final String str) {
        String[] words = StringUtils.splitByCharacterTypeCamelCase(str);
        return StringUtil.join(words, " ");
    }

    /**
     * Переводит текст, используя хак-скрипт
     *
     * @param text текст для перевода
     * @return перевод текста
     * @throws IOException ошибка обращения к хак-скрипту :(
     */
    private String translateText(final String text) throws IOException {
        final ResourceBundle pluginResources = ResourceBundle.getBundle("plugin");
        final String urlStr = MessageFormat.format(pluginResources.getString("url"),
                URLEncoder.encode(text, StandardCharsets.UTF_8.toString()),
                pluginResources.getString("lang_to"), pluginResources.getString("lang_from"));
        URL url = new URL(urlStr);
        StringBuilder response = new StringBuilder();

        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setRequestProperty("User-Agent", "Mozilla/5.0");
        final BufferedReader in = new BufferedReader(new InputStreamReader(connection.getInputStream()));

        String inputLine;
        while ((inputLine = in.readLine()) != null) {
            response.append(inputLine);
        }
        in.close();

        return new String(response.toString().getBytes(), StandardCharsets.UTF_8);
    }
}
