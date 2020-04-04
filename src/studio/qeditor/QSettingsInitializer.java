package studio.qeditor;

import java.awt.RenderingHints;
import java.util.HashMap;
import java.util.Map;
import org.netbeans.editor.BaseKit;
import org.netbeans.editor.Settings;
import org.netbeans.editor.SettingsNames;
import org.netbeans.editor.SettingsUtil;
import org.netbeans.editor.TokenContext;

public class QSettingsInitializer
        extends Settings.AbstractInitializer
{
    private static String Q_PREFIX="q-settings-initializer"; // NOI18N

    public QSettingsInitializer()
    {
        super(Q_PREFIX);
    }

    public void updateSettingsMap(Class kitClass,Map settingsMap)
    {
        if(kitClass==BaseKit.class)
        {
            QTokenColoringInitializer colorInitializer=new QTokenColoringInitializer();
            colorInitializer.updateSettingsMap(kitClass,settingsMap);
        }

        if(kitClass==QKit.class)
        {
            settingsMap.put(SettingsNames.LINE_NUMBER_VISIBLE,Boolean.TRUE);

            HashMap hints=new HashMap();
            // TODO: Detect if JDK6.0 and, if so, update VALUE_TEXT_ANTIALIAS_ON with other settings
            hints.put(RenderingHints.KEY_TEXT_ANTIALIASING,RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            settingsMap.put(SettingsNames.RENDERING_HINTS,hints);
            SettingsUtil.updateListSetting(
                    settingsMap,
                    SettingsNames.TOKEN_CONTEXT_LIST,
                    new TokenContext[]
                    {
                        QTokenContext.context
                    });
        }
    }
}
