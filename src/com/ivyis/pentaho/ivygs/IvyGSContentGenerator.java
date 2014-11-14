package com.ivyis.pentaho.ivygs;

import pt.webdetails.cpf.SimpleContentGenerator;
import pt.webdetails.cpf.utils.PluginUtils;

/**
 * @author <a href="mailto:joel.latino@ivy-is.co.uk">Joel Latino</a>
 * @since 1.0.0
 */
public class IvyGSContentGenerator extends SimpleContentGenerator {

  @Override
  public String getPluginName() {
    return new PluginUtils().getPluginName();
  }
}
