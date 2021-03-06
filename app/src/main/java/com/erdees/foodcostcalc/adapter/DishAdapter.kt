package com.erdees.foodcostcalc.adapter

import android.annotation.SuppressLint
import android.app.Activity
import android.app.AlertDialog
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.core.content.ContextCompat
import androidx.core.text.isDigitsOnly
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.FragmentManager
import androidx.lifecycle.LifecycleOwner
import androidx.recyclerview.widget.RecyclerView
import com.erdees.foodcostcalc.Constants
import com.erdees.foodcostcalc.Constants.DISH_AD_ITEM_TYPE
import com.erdees.foodcostcalc.R
import com.erdees.foodcostcalc.SharedFunctions.calculatePrice
import com.erdees.foodcostcalc.SharedFunctions.formatPrice
import com.erdees.foodcostcalc.SharedFunctions.getListSize
import com.erdees.foodcostcalc.fragments.dialogs.AddProductToDish
import com.erdees.foodcostcalc.fragments.dialogs.EditDish
import com.erdees.foodcostcalc.model.GrandDish
import com.erdees.foodcostcalc.viewmodel.adaptersViewModel.DishAdapterViewModel
import com.erdees.foodcostcalc.viewmodel.adaptersViewModel.DishListViewAdapterViewModel
import com.erdees.foodcostcalc.views.MaskedItemView
import com.google.android.play.core.review.ReviewManagerFactory
import io.reactivex.subjects.PublishSubject
import java.util.ArrayList
import com.erdees.foodcostcalc.Constants.DISH_ITEM_TYPE
import com.erdees.foodcostcalc.Constants.LAST_ITEM_TYPE
import com.erdees.foodcostcalc.ads.AdHelper
import com.google.android.gms.ads.AdListener
import com.google.android.gms.ads.AdLoader
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.VideoOptions
import com.google.android.gms.ads.formats.NativeAdOptions.ADCHOICES_BOTTOM_LEFT
import com.google.android.gms.ads.formats.NativeAdOptions.ADCHOICES_TOP_LEFT
import com.google.android.gms.ads.nativead.AdChoicesView
import com.google.android.gms.ads.nativead.NativeAd
import com.google.android.gms.ads.nativead.NativeAdOptions
import com.google.android.gms.ads.nativead.NativeAdView


class DishAdapter(
    val tag: String?,
    private val list: ArrayList<GrandDish>,
    private val fragmentManager: FragmentManager,
    val viewModel: DishAdapterViewModel,
    private val dishListViewAdapterViewModel: DishListViewAdapterViewModel,
    val viewLifecycleOwner: LifecycleOwner,
    private val activity: Activity
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    private val adCase = AdHelper(list.size, Constants.DISHES_AD_FREQUENCY)

    private val itemsSizeWithAds = adCase.newListSizeWithAds + 1 // +1 to include button as footer.

    private val positionsOfAds = adCase.positionsOfAds()

    private var currentNativeAd: NativeAd? = null


    inner class DishRecyclerViewHolder(view: View) : RecyclerView.ViewHolder(view) {


        private val eachLinearLayout: LinearLayout = view.findViewById(R.id.linear_layout_dish_card)
        private val dishNameTextView: TextView = view.findViewById(R.id.dish_name_in_adapter)
        private val dishMarginTextView: TextView = view.findViewById(R.id.dish_margin_in_adapter)
        private val dishTaxTextView: TextView = view.findViewById(R.id.dish_tax_in_adapter)
        private val editButton: ImageButton = view.findViewById(R.id.edit_button_in_dish_adapter)
        private val addProductsButton: ImageButton =
            view.findViewById(R.id.add_product_to_dish_button)
        private val listView: ListView = view.findViewById(R.id.list_view)
          private val totalPriceOfDish: TextView = view.findViewById(R.id.total_price_dish_card_view)
          private val finalPriceWithMarginAndTax: TextView =
            view.findViewById(R.id.total_price_with_margin_dish_card_view)
        private val howManyServingsTV =
            view.findViewById<TextView>(R.id.how_many_servings_text_view)
        private var totalPrice: Double = 0.0
        private var dishMargin : Double = 0.0
        private var dishTax : Double = 0.0
        private var amountOfServingsToPresent = 1
        private val amountOfServingsSubject = PublishSubject.create<Int>()

        private fun setPriceData(amountOfServings : Int){
            totalPriceOfDish.text = formatPrice(totalPrice * amountOfServings)
            finalPriceWithMarginAndTax.text = formatPrice(
                countPriceAfterMarginAndTax(
                    totalPrice,
                    dishMargin,
                    dishTax,
                    amountOfServings
                )
            )
        }

        @SuppressLint("SetTextI18n")
        private fun setHowManyServingsTV(amountOfServings: Int) {
            if(amountOfServings == 1 ) howManyServingsTV.text = "Data per serving."
            else howManyServingsTV.text = "Data for $amountOfServings servings."
        }
        /**Summing up total price of products included and then one by one adding price of each half product.*/
        private fun sumPriceAndSetPriceData(position: Int){
            list[position].halfProducts.forEach {
                viewModel
                    .getCertainHalfProductWithProductsIncluded(it.halfProductOwnerId)
                    .observe(viewLifecycleOwner, { halfProductWithProductsIncluded ->
                        val totalPriceOfThisHalfProduct = calculatePrice(
                            halfProductWithProductsIncluded.pricePerUnit(),
                            it.weight,
                            halfProductWithProductsIncluded.halfProduct.halfProductUnit,
                            it.unit)
                        totalPrice += totalPriceOfThisHalfProduct
                        if(isThisLastItemOfTheList(list[position].halfProducts.indexOf(it),list[position].halfProducts.size)) setPriceData(amountOfServingsToPresent)
                    })
            }
        }

        @SuppressLint("SetTextI18n")
        private fun setNameTaxAndMarginAccordingly(position: Int){
            dishNameTextView.text = list[position].dish.name
            dishMarginTextView.text = "Margin: ${list[position].dish.marginPercent}%"
            dishTaxTextView.text = "Tax: ${list[position].dish.dishTax}%"
        }

        private fun setEditDishButton(position: Int){
            editButton.setOnClickListener {
                EditDish().show(fragmentManager, EditDish.TAG)
                EditDish.dishPassedFromAdapter = list[position]
            }
        }

        private fun setAddProductsButton(position: Int){
            addProductsButton.setOnClickListener {
                viewModel.passDishToDialog(list[position].dish)
                openDialog(AddProductToDish())
            }
        }

        private fun setWholeLayoutAsListenerWhichOpensAndClosesListOfProducts(position: Int){
            eachLinearLayout.setOnClickListener {
                if (listView.adapter == null) {
                    howManyServingsTV.visibility = View.VISIBLE
                    listView.adapter = makeAdapterForList(position, amountOfServingsToPresent)
                    val indicesOfBothLists = list[position].productsIncluded.indices + list[position].halfProducts.indices
                    listView.layoutParams =
                        LinearLayout.LayoutParams(listView.layoutParams.width, getListSize(indicesOfBothLists,listView))
                } else {
                    howManyServingsTV.visibility = View.GONE
                    listView.adapter = null
                    listView.layoutParams =
                        LinearLayout.LayoutParams(listView.layoutParams.width, 0)
                }
            }
        }

        private fun setPositiveButtonFunctionality(button: Button, editText: EditText, alertDialog: AlertDialog, position: Int){
            button.setOnClickListener {
                if(editText.text.isNullOrBlank() || !editText.text.isDigitsOnly()){
                    Toast.makeText(activity,"Wrong input!",Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }
                amountOfServingsSubject.onNext(editText.text.toString().toInt())
                listView.adapter =
                    makeAdapterForList(position, amountOfServingsToPresent) // TO REFRESH LIST
                val indicesOfBothLists = list[position].productsIncluded.indices + list[position].halfProducts.indices
                listView.layoutParams =
                    LinearLayout.LayoutParams(listView.layoutParams.width, getListSize(indicesOfBothLists,listView)) // TO CHANGE SIZE TOO
                alertDialog.dismiss()
            }
        }

        private fun setHowManyServingsTvAsButton(textView: TextView,position: Int){
            textView.setOnClickListener {
            val textInputLayout = activity.layoutInflater.inflate(R.layout.text_input_layout,null)
            val editText = textInputLayout.findViewById<EditText>(R.id.text_input_layout_quantity)
            val linearLayout = LinearLayout(activity)
            val params: LinearLayout.LayoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT
            )
            params.setMargins(25, 0, 25, 0)
            editText.setText(amountOfServingsToPresent.toString())
            linearLayout.addView(textInputLayout, params)
            val alertDialog = AlertDialog.Builder(activity)
                .setMessage("Serving amount")
                .setView(linearLayout)
                .setPositiveButton("Submit", null)
                .setNegativeButton("Back", null)
                .show()
            alertDialog.window?.setBackgroundDrawable(
                ContextCompat.getDrawable(
                    activity,
                    R.drawable.background_for_dialogs
                )
            )
            val positiveButton = alertDialog.getButton(AlertDialog.BUTTON_POSITIVE)
            setPositiveButtonFunctionality(positiveButton,editText,alertDialog,position)
        }}

        private fun setDishData(position: Int){
            totalPrice = list[position].totalPrice
            dishMargin = list[position].dish.marginPercent
            dishTax = list[position].dish.dishTax
        }

        @SuppressLint("CheckResult")
        fun bind(position: Int) {
            val positionIncludedAdsBinded = adCase.correctElementFromListToBind(position)

            if (position == 3) openFeedBackForm()
            setDishData(positionIncludedAdsBinded)
            amountOfServingsSubject.subscribe { i ->
                amountOfServingsToPresent = i
                setHowManyServingsTV(i)
                setPriceData(amountOfServingsToPresent)
            }
            amountOfServingsSubject.onNext(1)
            sumPriceAndSetPriceData(positionIncludedAdsBinded)
            setNameTaxAndMarginAccordingly(positionIncludedAdsBinded)
            setEditDishButton(positionIncludedAdsBinded)
            setAddProductsButton(positionIncludedAdsBinded)
            setWholeLayoutAsListenerWhichOpensAndClosesListOfProducts(positionIncludedAdsBinded)
            setHowManyServingsTvAsButton(howManyServingsTV,positionIncludedAdsBinded)
        }
    }


    inner class AdItemViewHolder(val view: NativeAdView) : RecyclerView.ViewHolder(view) {
        fun bind() {
            val builder = AdLoader.Builder(activity, Constants.ADMOB_DISHES_RV_AD_UNIT_ID)
            builder.forNativeAd { nativeAd ->
                // OnUnifiedNativeAdLoadedListener implementation.
                // If this callback occurs after the activity is destroyed, you must call
                // destroy and return or you may get a memory leak.
                val activityDestroyed: Boolean = activity.isDestroyed
                if (activityDestroyed || activity.isFinishing || activity.isChangingConfigurations) {
                    nativeAd.destroy()
                    Log.i("TESTTT", " THIS RETURN IS CALLED ")
                    return@forNativeAd
                }
                // You must call destroy on old ads when you are done with them,
                // otherwise you will have a memory leak.
                currentNativeAd?.destroy()
                currentNativeAd = nativeAd
                adCase.populateNativeAdView(nativeAd, view)
            }
            val videoOptions = VideoOptions.Builder()
                .build()
            val adOptions = NativeAdOptions.Builder()
                .setVideoOptions(videoOptions)
                .build()
            builder.withNativeAdOptions(adOptions)
            val adLoader = builder.withAdListener(object : AdListener() {
            }).build()
            adLoader.loadAd(AdRequest.Builder().build())
        }
    }

    inner class LastItemViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        private val layoutAsButton: MaskedItemView =
            view.findViewById(R.id.products_last_item_layout)
        private val text: TextView = view.findViewById(R.id.last_item_text)

        @SuppressLint("SetTextI18n")
        fun bind() {
            text.text = "Create Dish"
            layoutAsButton.setOnClickListener {
                viewModel.setOpenCreateDishFlag(true)
                viewModel.setOpenCreateDishFlag(false)
            }
        }

    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return when (viewType) {
            DISH_ITEM_TYPE -> {
                val adapterLayout =
                    LayoutInflater.from(parent.context)
                        .inflate(R.layout.dish_card_view, parent, false)
                DishRecyclerViewHolder(adapterLayout)
            }
            DISH_AD_ITEM_TYPE -> {
                val adapterLayout = LayoutInflater.from(parent.context)
                    .inflate(R.layout.product_ad_custom_layout, parent, false) as NativeAdView
                AdItemViewHolder(adapterLayout)
            }
            else -> {
                val adapterLayout =
                    LayoutInflater.from(parent.context)
                        .inflate(R.layout.products_recycler_last_item, parent, false)
                LastItemViewHolder(adapterLayout)
            }
        }


    }

    override fun getItemCount(): Int {
        return itemsSizeWithAds
    }

    override fun getItemViewType(position: Int): Int {
        return if (position == itemsSizeWithAds - 1) LAST_ITEM_TYPE
        else if (positionsOfAds.contains(position)) DISH_AD_ITEM_TYPE
        else  DISH_ITEM_TYPE
    }

    @SuppressLint("WrongConstant", "ShowToast", "SetTextI18n")
    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        if (holder.itemViewType == DISH_ITEM_TYPE) (holder as DishAdapter.DishRecyclerViewHolder).bind(
            position
        )
        else if (holder.itemViewType == DISH_AD_ITEM_TYPE) (holder as AdItemViewHolder).bind()
        else (holder as LastItemViewHolder).bind()
    }


    private fun openDialog(dialog: DialogFragment) {
        val transaction = fragmentManager.beginTransaction()
        transaction.addToBackStack(dialog.tag)
        dialog.show(transaction, TAG)
    }

    private fun makeAdapterForList(position: Int, servings: Int): ListAdapter {
        return DishListViewAdapter(
            activity,
            list[position],
            servings,
            dishListViewAdapterViewModel,
            viewLifecycleOwner
        )
    }


    private fun openFeedBackForm() {
        Log.i("Main Activity", "review triggered!")
        val manager = ReviewManagerFactory.create(activity)
        val request = manager.requestReviewFlow()
        request.addOnCompleteListener { thisRequest ->
            if (thisRequest.isSuccessful) {
                val reviewInfo = thisRequest.result
                val flow = manager.launchReviewFlow(activity, reviewInfo)
                flow.addOnCompleteListener {
                    //Continue your application process
                    Log.i("Main Activity", "review success!")
                }
            } else {
                //Handle the error here
                Log.i("Main Activity", "review fail!")

            }
        }
    }

    companion object {
        const val TAG = "DishAdapter"
    }



    private fun isThisLastItemOfTheList(indexOfHalfProduct : Int , listSize : Int):Boolean{
        return (indexOfHalfProduct == listSize -1)
    }

     fun countPriceAfterMarginAndTax(
        totalPrice: Double,
        margin: Double,
        tax: Double,
        amountOfServings: Int
    ): Double {
        val priceWithMargin = totalPrice * margin / 100
        val amountOfTax = priceWithMargin * tax / 100
        return (priceWithMargin + amountOfTax) * amountOfServings
    }


}