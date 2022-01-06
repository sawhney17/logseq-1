(ns frontend.components.header
  (:require ["path" :as path]
            [cljs-bean.core :as bean]
            [frontend.components.export :as export]
            [frontend.components.page-menu :as page-menu]
            [frontend.components.plugins :as plugins]
            [frontend.components.repo :as repo]
            [frontend.components.right-sidebar :as sidebar]
            [frontend.components.svg :as svg]
            [frontend.components.widgets :as widgets]
            [frontend.config :as config]
            [frontend.context.i18n :as i18n]
            [frontend.fs.sync :as fs-sync]
            [frontend.handler :as handler]
            [frontend.handler.file-sync :as file-sync-handler]
            [frontend.handler.page :as page-handler]
            [frontend.handler.plugin :as plugin-handler]
            [frontend.handler.route :as route-handler]
            [frontend.handler.user :as user-handler]
            [frontend.handler.web.nfs :as nfs]
            [frontend.handler.web.nfs :as nfs-handler]
            [frontend.mobile.util :as mobile-util]
            [frontend.modules.shortcut.core :as shortcut]
            [frontend.state :as state]
            [frontend.ui :as ui]
            [frontend.util :as util]
            [reitit.frontend.easy :as rfe]
            [rum.core :as rum]
            [frontend.mobile.util :as mobile-util]
            [frontend.fs.sync :as fs-sync]
            [cljs.core.async :as a]))

(rum/defc home-button []
  (ui/with-shortcut :go/home "left"
    [:a.button
     {:href     (rfe/href :home)
      :on-click route-handler/go-to-journals!}
     (ui/icon "home" {:style {:fontSize ui/icon-size}})]))

(rum/defc login < rum/reactive
  []
  (let [_ (state/sub :auth/id-token)])
  (rum/with-context [[t] i18n/*tongue-context*]
    (when-not config/publishing?
      (if (user-handler/logged?)
        [:span.text-sm.font-medium (user-handler/email)]

        [:a.button.text-sm.font-medium.block {:on-click (fn []
                                                          (js/window.open "https://logseq-test.auth.us-east-2.amazoncognito.com/oauth2/authorize?client_id=4fi79en9aurclkb92e25hmu9ts&response_type=code&scope=email+openid+phone&redirect_uri=logseq%3A%2F%2Fauth-callback"))}
         [:span (t :login)]]))))

(rum/defcs file-sync <
  rum/reactive
  (rum/local nil ::existed-graphs)
  [state]
  (let [_ (state/sub :auth/id-token)
        _ (state/sub :file-sync/sync-state)
        ^fs-sync/SyncState sync-state (state/get-file-sync-state-manager)
        not-syncing? (or (nil? sync-state) (fs-sync/-stopped? sync-state))
        *existed-graphs (::existed-graphs state)
        _ (rum/react file-sync-handler/refresh-file-sync-component)
        graph-txid-exists? (file-sync-handler/graph-txid-exists?)]
    (when-not config/publishing?
      (when (user-handler/logged?)
        (when-not (file-sync-handler/graph-txid-exists?)
          (a/go
            (let [graphs (a/<! (file-sync-handler/list-graphs))]
              (reset! *existed-graphs graphs))))
        (ui/dropdown-with-links
         (fn [{:keys [toggle-fn]}]
           (if not-syncing?
             [:a.button
              {:on-click toggle-fn}
              (ui/icon "cloud-off" {:style {:fontSize ui/icon-size}})]
             [:a.button
              {:on-click toggle-fn}
              (ui/icon "cloud" {:style {:fontSize ui/icon-size}})]))
         (cond-> []
           (not graph-txid-exists?)
           (concat (->> @*existed-graphs
                        (filterv #(and (:GraphName %) (:GraphUUID %)))
                        (mapv (fn [graph]
                                {:title (:GraphName graph)
                                 :options {:on-click #(file-sync-handler/switch-graph (:GraphUUID graph))}})))
                   [{:hr true}
                    {:title "create graph"
                     :options {:on-click #(file-sync-handler/create-graph (path/basename (state/get-current-repo)))}}])
           graph-txid-exists?
           (conj {:title "toggle file sync"
                  :options {:on-click #(if not-syncing? (fs-sync/sync-start) (fs-sync/sync-stop))}}))

         (cond-> {}
           (not graph-txid-exists?) (assoc :links-header [:div.font-medium.text-sm.opacity-60.px-4.pt-2
                                                          "Switch to:"])))))))

(rum/defc left-menu-button < rum/reactive
  [{:keys [on-click]}]
  (ui/with-shortcut :ui/toggle-left-sidebar "bottom"
    [:a#left-menu.cp__header-left-menu.button
     {:on-click on-click
      :style {:margin-left 12}}
     (ui/icon "menu-2" {:style {:fontSize ui/icon-size}})]))

(rum/defc dropdown-menu < rum/reactive
  [{:keys [current-repo t]}]
  (let [logged? (user-handler/logged?)
        page-menu (page-menu/page-menu nil)
        page-menu-and-hr (when (seq page-menu)
                           (concat page-menu [{:hr true}]))]
    (ui/dropdown-with-links
     (fn [{:keys [toggle-fn]}]
       [:a.button
        {:on-click toggle-fn}
        (ui/icon "dots" {:style {:fontSize ui/icon-size}})])
     (->>
      [(when-not (state/publishing-enable-editing?)
         {:title (t :settings)
          :options {:on-click state/open-settings!}
          :icon (ui/icon "settings")})

       (when plugin-handler/lsp-enabled?
         {:title (t :plugins)
          :options {:on-click #(plugin-handler/goto-plugins-dashboard!)}
          :icon (ui/icon "apps")})

       (when plugin-handler/lsp-enabled?
         {:title (t :themes)
          :options {:on-click #(plugins/open-select-theme!)}
          :icon (ui/icon "palette")})

       (when current-repo
         {:title (t :export-graph)
          :options {:on-click #(state/set-modal! export/export)}
          :icon (ui/icon "database-export")})

       (when current-repo
         {:title (t :import)
          :options {:href (rfe/href :import)}
          :icon (ui/icon "file-upload")})

       {:title [:div.flex-row.flex.justify-between.items-center
                [:span (t :join-community)]]
        :options {:href "https://discord.gg/KpN4eHY"
                  :title (t :discord-title)
                  :target "_blank"}
        :icon (ui/icon "brand-discord")}
       (when logged?
         {:title (t :sign-out)
          :options {:on-click user-handler/sign-out!}
          :icon svg/logout-sm})]
      (concat page-menu-and-hr)
      (remove nil?))
     {}
     ;; {:links-footer (when (and (util/electron?) (not logged?))
     ;;                  [:div.px-2.py-2 (login logged?)])}
     )))

(rum/defc back-and-forward
  []
  [:div.flex.flex-row

   (ui/with-shortcut :go/backward "bottom"
     [:a.it.navigation.nav-left.button
      {:title "Go back" :on-click #(js/window.history.back)}
      (ui/icon "arrow-left" {:style {:fontSize ui/icon-size}})])

   (ui/with-shortcut :go/forward "bottom"
     [:a.it.navigation.nav-right.button
      {:title "Go forward" :on-click #(js/window.history.forward)}
      (ui/icon "arrow-right" {:style {:fontSize ui/icon-size}})])])

(rum/defc updater-tips-new-version
  [t]
  (let [[downloaded, set-downloaded] (rum/use-state nil)
        _ (rum/use-effect!
            (fn []
              (when-let [channel (and (util/electron?) "auto-updater-downloaded")]
                (let [callback (fn [_ args]
                                 (js/console.debug "[new-version downloaded] args:" args)
                                 (let [args (bean/->clj args)]
                                   (set-downloaded args)
                                   (state/set-state! :electron/auto-updater-downloaded args))
                                 nil)]
                  (js/apis.addListener channel callback)
                  #(js/apis.removeListener channel callback))))
            [])]

    (when downloaded
      [:div.cp__header-tips
       [:p (t :updater/new-version-install)
        [:a.restart.ml-2
         {:on-click #(handler/quit-and-install-new-version!)}
         (svg/reload 16) [:strong (t :updater/quit-and-install)]]]])))

(rum/defc header < rum/reactive
  [{:keys [open-fn current-repo default-home new-block-mode]}]
  (let [repos (->> (state/sub [:me :repos])
                   (remove #(= (:url %) config/local-repo)))
        electron-mac? (and util/mac? (util/electron?))
        vw-state (state/sub :ui/visual-viewport-state)
        show-open-folder? (and (or (nfs/supported?)
                                   (mobile-util/is-native-platform?))
                               (empty? repos)
                               (not config/publishing?))
        refreshing? (state/sub :nfs/refreshing?)]
    (rum/with-context [[t] i18n/*tongue-context*]
      [:div.cp__header#head
       {:class           (util/classnames [{:electron-mac   electron-mac?
                                            :native-ios     (mobile-util/native-ios?)
                                            :native-android (mobile-util/native-android?)}])
        :on-double-click (fn [^js e]
                           (when-let [target (.-target e)]
                             (when (and (util/electron?)
                                        (.. target -classList (contains "cp__header")))
                               (js/window.apis.toggleMaxOrMinActiveWindow))))
        :style           {:fontSize  50
                          :transform (str "translateY(" (or (:offset-top vw-state) 0) "px)")}}
       [:div.l.flex
        (left-menu-button {:on-click (fn []
                                       (open-fn)
                                       (state/set-left-sidebar-open!
                                        (not (:ui/left-sidebar-open? @state/state))))})

        (when current-repo ;; this is for the Search button
          (ui/with-shortcut :go/search "right"
            [:a.button#search-button
             {:on-click #(do (when (or (mobile-util/native-android?)
                                       (mobile-util/native-iphone?))
                               (state/set-left-sidebar-open! false))
                             (state/pub-event! [:go/search]))}
             (ui/icon "search" {:style {:fontSize ui/icon-size}})]))]

       [:div.r.flex
        (file-sync)
        (login)
        (when plugin-handler/lsp-enabled?
          (plugins/hook-ui-items :toolbar))

        (when (not= (state/get-current-route) :home)
          (home-button))

        (when (or (util/electron?)
                  (mobile-util/native-ios?))
          (back-and-forward))

        (new-block-mode)

        (when (and (mobile-util/is-native-platform?) (seq repos))
          [:a.text-sm.font-medium.button
           {:on-click
            (fn []
              (state/pub-event!
               [:modal/show
                [:div {:style {:max-width 700}}
                 [:p "Refresh detects and processes files modified on your disk and diverged from the actual Logseq page content. Continue?"]
                 (ui/button
                  "Yes"
                  :on-click (fn []
                              (state/close-modal!)
                              (nfs/refresh! (state/get-current-repo) repo/refresh-cb)))]]))}
           (if refreshing?
             [:div {:class "animate-spin-reverse"}
              svg/refresh]
             [:div.flex.flex-row.text-center.open-button__inner.items-center
              (ui/icon "refresh" {:style {:fontSize ui/icon-size}})])])

        (repo/sync-status current-repo)

        (when (and
               show-open-folder?
               (not (mobile-util/is-native-platform?)))
          [:a.text-sm.font-medium.button
           {:on-click #(page-handler/ls-dir-files! shortcut/refresh!)}
           [:div.flex.flex-row.text-center.open-button__inner.items-center
            (ui/icon "folder-plus")
            (when-not config/mobile?
              [:span.ml-1 {:style {:margin-top (if electron-mac? 0 2)}}
               (t :open)])]])

        (when config/publishing?
          [:a.text-sm.font-medium.button {:href (rfe/href :graph)}
           (t :graph)])

        (dropdown-menu {:t            t
                        :current-repo current-repo
                        :default-home default-home})

        (when (not (state/sub :ui/sidebar-open?))
          (sidebar/toggle))

        (updater-tips-new-version t)]])))
