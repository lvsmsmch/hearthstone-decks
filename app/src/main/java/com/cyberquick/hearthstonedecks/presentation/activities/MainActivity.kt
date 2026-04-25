package com.cyberquick.hearthstonedecks.presentation.activities

import android.animation.ValueAnimator
import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.view.animation.DecelerateInterpolator
import android.widget.TextView
import androidx.appcompat.app.ActionBar
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import com.cyberquick.hearthstonedecks.BuildConfig
import com.cyberquick.hearthstonedecks.R
import com.cyberquick.hearthstonedecks.databinding.ActivityMainBinding
import com.cyberquick.hearthstonedecks.domain.repositories.SetsRepository
import com.cyberquick.hearthstonedecks.presentation.common.ToolbarHolder
import com.cyberquick.hearthstonedecks.presentation.fragments.*
import com.cyberquick.hearthstonedecks.utils.CustomAppReviewer
import com.cyberquick.hearthstonedecks.utils.Event
import com.cyberquick.hearthstonedecks.utils.Preferences
import com.cyberquick.hearthstonedecks.utils.logFirebaseEvent
import com.cyberquick.hearthstonedecks.utils.simpleNavigate
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject


@AndroidEntryPoint
class MainActivity : AppCompatActivity(), ToolbarHolder {

    @Inject
    lateinit var setsRepository: SetsRepository

    private lateinit var binding: ActivityMainBinding
    private lateinit var drawerToggle: ActionBarDrawerToggle

    private enum class HomeButton { Menu, Back; }

    private var homeButton = HomeButton.Menu

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        initToolbar()
        initNavigationDrawer()
        initHomeButtonIcon()
        doOtherStuff()

        simpleNavigate(OnlineStandardPageFragment())

        handleIntent(intent)
    }

    private fun initToolbar() {
        setSupportActionBar(binding.toolbar)
        (supportActionBar as ActionBar).apply {
            setDisplayHomeAsUpEnabled(true)
            setDisplayShowHomeEnabled(true)
        }

        drawerToggle = ActionBarDrawerToggle(
            this,
            binding.drawerLayout,
            binding.toolbar,
            R.string.open_menu_go_back,
            R.string.open_menu_go_back,
        )
        drawerToggle.isDrawerIndicatorEnabled = true
        drawerToggle.syncState()

        binding.toolbar.setNavigationOnClickListener {
            when (homeButton) {
                HomeButton.Menu -> {
                    logFirebaseEvent(this, Event.TOOLBAR_CLICK_MENU)
                    binding.drawerLayout.openDrawer(binding.navigationDrawer)
                }

                HomeButton.Back -> {
                    logFirebaseEvent(this, Event.TOOLBAR_CLICK_BACK)
                    onBackPressed()
                }
            }
        }
    }

    @SuppressLint("SetTextI18n")
    private fun initNavigationDrawer() {
        binding.navigationDrawer.itemIconTintList = null
        binding.navigationDrawer.setNavigationItemSelectedListener { menuItem ->
            binding.drawerLayout.closeDrawer(binding.navigationDrawer)
            when (menuItem.itemId) {
                R.id.drawer_menu_item_standard_decks -> {
                    simpleNavigate(OnlineStandardPageFragment())
                    logFirebaseEvent(this, Event.DRAWER_CLICK_DECKS_STANDARD)
                }

                R.id.drawer_menu_item_wild_decks -> {
                    simpleNavigate(OnlineWildPageFragment())
                    logFirebaseEvent(this, Event.DRAWER_CLICK_DECKS_WILD)
                }

                R.id.drawer_menu_item_my_decks -> {
                    simpleNavigate(FavoritePageFragment())
                    logFirebaseEvent(this, Event.DRAWER_CLICK_DECKS_SAVED)
                }

                R.id.drawer_menu_item_about -> {
                    simpleNavigate(AboutAppFragment())
                    logFirebaseEvent(this, Event.DRAWER_CLICK_ABOUT_APP)
                }
            }
            return@setNavigationItemSelectedListener true
        }

        val versionText = getString(R.string.version_ph, BuildConfig.VERSION_NAME)
        binding.navigationDrawer.getHeaderView(0)
            .findViewById<TextView>(R.id.tv_app_version).text = versionText
    }

    private fun initHomeButtonIcon() {
        supportFragmentManager.registerFragmentLifecycleCallbacks(
            object : FragmentManager.FragmentLifecycleCallbacks() {
                override fun onFragmentViewCreated(
                    fm: FragmentManager, fragment: Fragment,
                    v: View, savedInstanceState: Bundle?
                ) {
                    super.onFragmentViewCreated(fm, fragment, v, savedInstanceState)
                    val newHomeButton = when (fragment) {
                        is DeckFragment, is AboutAppFragment -> HomeButton.Back
                        else -> HomeButton.Menu
                    }
                    if (newHomeButton == homeButton) return
                    homeButton = newHomeButton
                    val anim = when (newHomeButton) {
                        HomeButton.Back -> ValueAnimator.ofFloat(0f, 1f)
                        HomeButton.Menu -> ValueAnimator.ofFloat(1f, 0f)
                    }

                    anim.addUpdateListener { valueAnimator ->
                        val slideOffset = valueAnimator.animatedValue as Float
                        drawerToggle.onDrawerSlide(binding.drawerLayout, slideOffset)
                    }
                    anim.interpolator = DecelerateInterpolator()
                    anim.duration = 200     // 400
                    anim.start()
                }
            }, false
        )
    }

    private fun showExitWindow() {
        logFirebaseEvent(this, Event.DIALOG_EXIT_SHOW)
        MaterialAlertDialogBuilder(this, R.style.AlertDialogTheme)
            .setTitle(R.string.quit_app_question)
            .setPositiveButton(R.string.quit) { dialog, _ ->
                logFirebaseEvent(this, Event.DIALOG_EXIT_YES)
                dialog.dismiss()
                finish()
            }
            .setNegativeButton(R.string.cancel) { dialog, _ ->
                logFirebaseEvent(this, Event.DIALOG_EXIT_NO)
                dialog.dismiss()
            }
            .show()
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleIntent(intent)
    }

    private fun handleIntent(intent: Intent) {
        when (intent.action) {
            Intent.ACTION_MAIN -> {
                logFirebaseEvent(this, Event.OPEN_APP)
                Preferences.getInstance(this).increaseAmountOfTimesAppLaunch()

                if (CustomAppReviewer.shouldShow(this)) {
                    CustomAppReviewer(this).ask()
                }
            }
        }
    }

    private fun doOtherStuff() {
        setsRepository.refreshSets()
    }

    private fun setHomeButtonAs(newButton: HomeButton) {
        if (newButton == homeButton) return
        homeButton = newButton
        val anim = when (newButton) {
            HomeButton.Back -> ValueAnimator.ofFloat(0f, 1f)
            HomeButton.Menu -> ValueAnimator.ofFloat(1f, 0f)
        }

        anim.addUpdateListener { valueAnimator ->
            val slideOffset = valueAnimator.animatedValue as Float
            drawerToggle.onDrawerSlide(binding.drawerLayout, slideOffset)
        }
        anim.interpolator = DecelerateInterpolator()
        anim.duration = 200     // 400
        anim.start()
    }


    override fun showToolbar() {
        binding.toolbar.isVisible = true
    }

    override fun hideToolbar() {
        binding.toolbar.isVisible = false
    }

    override fun showHomeButtonAsMenu() {
        setHomeButtonAs(HomeButton.Menu)
    }

    override fun showHomeButtonAsBack() {
        setHomeButtonAs(HomeButton.Back)
    }

    override fun setText(text: String) {
        binding.toolbar.title = text
    }


    override fun onBackPressed() {
        if (supportFragmentManager.backStackEntryCount > 1) {
            supportFragmentManager.popBackStack()
        } else {
            finish()
        }
    }
}