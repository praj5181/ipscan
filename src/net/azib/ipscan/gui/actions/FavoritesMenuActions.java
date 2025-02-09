/*
  This file is a part of Angry IP Scanner source code,
  see http://www.angryip.org/ for more information.
  Licensed under GPLv2.
 */
package net.azib.ipscan.gui.actions;

import net.azib.ipscan.config.FavoritesConfig;
import net.azib.ipscan.config.Labels;
import net.azib.ipscan.config.Version;
import net.azib.ipscan.core.UserErrorException;
import net.azib.ipscan.core.state.ScanningState;
import net.azib.ipscan.core.state.StateMachine;
import net.azib.ipscan.gui.EditFavoritesDialog;
import net.azib.ipscan.gui.InputDialog;
import net.azib.ipscan.gui.feeders.FeederGUIRegistry;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.widgets.*;

/**
 * FavoritesActions
 *
 * @author Anton Keks
 */
public class FavoritesMenuActions {

	public static final class Add implements Listener {
		private final FeederGUIRegistry feederRegistry;
		private final FavoritesConfig favoritesConfig;

		public Add(FavoritesConfig favoritesConfig, FeederGUIRegistry feederRegistry) {
			this.favoritesConfig = favoritesConfig;
			this.feederRegistry = feederRegistry;
		}
		
		public void handleEvent(Event event) {
			Shell activeShell = event.display.getActiveShell();
			String feederInfo = feederRegistry.current().getInfo();
			InputDialog inputDialog = new InputDialog(
					Labels.getLabel("title.favorite.add"), 
					Labels.getLabel("text.favorite.add"));
			String favoriteName = inputDialog.open(feederInfo);
			
			if (favoriteName != null) {
				if (favoritesConfig.get(favoriteName) != null) {
					throw new UserErrorException("favorite.alreadyExists");
				}
				favoritesConfig.add(favoriteName, feederRegistry.current());
				activeShell.setText(favoriteName + " - " + Version.NAME);
			}
		}
	}
	
	public static final class Select implements SelectionListener {
		private final FeederGUIRegistry feederRegistry;
		private final FavoritesConfig favoritesConfig;
		private final StartStopScanningAction startStopAction;

		public Select(FavoritesConfig favoritesConfig, FeederGUIRegistry feederRegistry, StartStopScanningAction startStopAction) {
			this.favoritesConfig = favoritesConfig;
			this.feederRegistry = feederRegistry;
			this.startStopAction = startStopAction;
		}

		public void widgetSelected(SelectionEvent event) {
			MenuItem menuItem = (MenuItem) event.widget;
			String key = menuItem.getText();
			
			feederRegistry.select(favoritesConfig.getFeederId(key));
			feederRegistry.current().unserialize(favoritesConfig.getSerializedParts(key));
			event.display.getActiveShell().setText(key + " - " + Version.NAME);
			
			// try to start scanning immediately
			startStopAction.widgetSelected(event);
		}

		public void widgetDefaultSelected(SelectionEvent e) {
			widgetSelected(e);
		}
	}
	
	public static final class Edit implements Listener {
		private final FavoritesConfig favoritesConfig;

		public Edit(FavoritesConfig favoritesConfig) {
			this.favoritesConfig = favoritesConfig;
		}

		public void handleEvent(Event event) {
			new EditFavoritesDialog(favoritesConfig).open();
		}
	}

	public static final class ShowMenu implements Listener {
		private final SelectionListener favoritesSelectListener;
		private final FavoritesConfig favoritesConfig;
		private final StateMachine stateMachine;

		public ShowMenu(FavoritesConfig favoritesConfig, Select favoritesSelectListener, StateMachine stateMachine) {
			this.favoritesConfig = favoritesConfig;
			// the listener for favorites selections from the menu
			this.favoritesSelectListener = favoritesSelectListener;
			this.stateMachine = stateMachine;
		}

		public void handleEvent(Event event) {
			Menu favoritesMenu = (Menu) event.widget;
			// populate favorites in the menu			
			// note: 3 is the number of items in the menu when no favorites exist
			
			// dispose old favorites
			MenuItem[] menuItems = favoritesMenu.getItems();
			for (int i = 3; i < menuItems.length; i++) {
				menuItems[i].dispose();
			}
			
			// update favorites menu items
			for (String name : favoritesConfig) {
				MenuItem menuItem = new MenuItem(favoritesMenu, SWT.CASCADE);
				menuItem.setText(name);
				menuItem.setEnabled(stateMachine.inCurrentState(ScanningState.IDLE));
				menuItem.addSelectionListener(favoritesSelectListener);
			}
		}
	}

}
