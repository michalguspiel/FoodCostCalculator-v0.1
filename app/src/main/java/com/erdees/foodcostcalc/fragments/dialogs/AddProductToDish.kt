@file:Suppress("PrivatePropertyName")

package com.erdees.foodcostcalc.fragments.dialogs

import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.util.Log
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.appcompat.widget.SwitchCompat
import androidx.core.text.isDigitsOnly
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.erdees.foodcostcalc.*
import com.erdees.foodcostcalc.SharedFunctions.changeUnitList
import com.erdees.foodcostcalc.SharedFunctions.hideKeyboard
import com.erdees.foodcostcalc.SharedFunctions.setAdapterList
import com.erdees.foodcostcalc.model.*
import com.erdees.foodcostcalc.viewmodel.AddProductToDishViewModel
import com.erdees.foodcostcalc.viewmodel.AddViewModel
import com.erdees.foodcostcalc.viewmodel.HalfProductsViewModel

class AddProductToDish : DialogFragment(), AdapterView.OnItemSelectedListener {
    private val PRODUCT_SPINNER_ID = 1
    private val DISH_SPINNER_ID = 2
    private val UNIT_SPINNER_ID = 3
    private var productPosition: Int? = null
    private var dishPosition: Int? = null
    private val unitList = arrayListOf<String>() // list for units, to populate spinner
    private var chosenUnit: String = ""

    private lateinit var switch: SwitchCompat
    private lateinit var halfProductToAdd: HalfProduct


    /** Initialized here so it can be called outside of 'onCreateView' */
    lateinit var viewModel: AddProductToDishViewModel
    private lateinit var unitAdapter: ArrayAdapter<*>
    private lateinit var unitSpinner: Spinner

    /**Holder for booleans*/
    private var metricAsBoolean = true
    private var usaAsBoolean = true

    /**Holder for type of units*/
    private var unitType = ""


    /**Spinner implementation */

    override fun onNothingSelected(parent: AdapterView<*>?) {
        this.dismiss()
    }

    override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
        when (parent?.id) {
            1 -> {
                productPosition = position
                if (!switch.isChecked) unitType = setAdapterList(
                        viewModel.readAllProductData.value?.get(position)?.unit
                )
                if (switch.isChecked) unitType = setAdapterList(
                        viewModel
                            .readAllHalfProductData.value?.get(position)?.halfProductUnit
                    )
                unitList.changeUnitList(unitType, metricAsBoolean, usaAsBoolean)
                unitAdapter.notifyDataSetChanged()
                unitSpinner.setSelection(0, false)
                chosenUnit = unitList.first()
                unitSpinner.setSelection(0) // when the product is chosen first units got chosen immediately
            }
            2 -> {
                dishPosition = position
            }
            else -> {
                chosenUnit = unitList[position]
                Log.i("test", chosenUnit)
            }
        }
    }


    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val view: View = inflater.inflate(R.layout.add_products_to_dish, container, false)

        Log.i("TEST", "Opened from ${this.tag}")
        /** initialize ui with viewmodel*/
        viewModel = ViewModelProvider(this).get(AddProductToDishViewModel::class.java)


        /** Get the data about unit settings from shared preferences.
         * true means that user uses certain units.
         * metricAsBoolean is set as true because something needs to be chosen in order for app to work.*/
        val sharedPreferences = SharedPreferences(requireContext())
        metricAsBoolean = sharedPreferences.getValueBoolean("metric", true)
        usaAsBoolean = sharedPreferences.getValueBoolean("usa", false)

        /** binders*/
        val weightOfAddedProduct = view.findViewById<EditText>(R.id.product_weight_in_half_product)
        weightOfAddedProduct.setOnFocusChangeListener { _, hasFocus ->
            if(!hasFocus) view.hideKeyboard()
        }
        val addProductToDishBtn =
            view.findViewById<ImageButton>(R.id.add_product_to_halfproduct_btn)
        val productSpinner = view.findViewById<Spinner>(R.id.mySpinner)
        val dishSpinner = view.findViewById<Spinner>(R.id.dishSpinner)
        switch = view.findViewById<SwitchCompat>(R.id.product_halfproduct_switch)
        val chooseProductTextView = view.findViewById<TextView>(R.id.choose_product_half_product)
        unitSpinner = view.findViewById(R.id.unitSpinner)


        /** ADAPTERs FOR SPINNERs */
        val halfProductList = mutableListOf<String>()
        viewModel.getHalfProducts()
            .observe(viewLifecycleOwner,
                { halfProducts ->
                    halfProducts.forEach { halfProductList.add(it.name) }
                })
        val halfProductAdapter =
            ArrayAdapter(requireActivity(), R.layout.spinner_layout, halfProductList)

        val productList = mutableListOf<String>()
        viewModel.readAllProductData.observe(
            viewLifecycleOwner,
            { it.forEach { product -> productList.add(product.name) } })
        val productAdapter = ArrayAdapter(requireActivity(), R.layout.spinner_layout, productList)
        with(productSpinner)
        {
            adapter = productAdapter
            setSelection(0, false)
            onItemSelectedListener = this@AddProductToDish
            prompt = "Select product"
            gravity = Gravity.CENTER
            id = PRODUCT_SPINNER_ID
        }
        productAdapter.notifyDataSetChanged()




        val dishList = mutableListOf<String>()
        viewModel.readAllDishData.observe(
            viewLifecycleOwner,
             {
                it.forEach { dish -> dishList.add(dish.name) }
                if(this.isOpenedFromDishAdapter()){
                    val dishToSelect =  viewModel.getDishToDialog().value
                    val positionToSelect = dishList.indexOf(dishToSelect!!.name)
                    dishSpinner.setSelection(positionToSelect)
                }
            })
        val dishesAdapter = ArrayAdapter(requireActivity(), R.layout.spinner_layout, dishList)
        with(dishSpinner) {
            adapter = dishesAdapter
            onItemSelectedListener = this@AddProductToDish
            prompt = "Select dish"
            gravity = Gravity.CENTER
            id = DISH_SPINNER_ID
        }
        dishesAdapter.notifyDataSetChanged()


        unitAdapter =
            ArrayAdapter(requireActivity(), R.layout.support_simple_spinner_dropdown_item, unitList)
        unitAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        with(unitSpinner) {
            adapter = unitAdapter
            setSelection(0, false)
            onItemSelectedListener = this@AddProductToDish
            prompt = "Select unit"
            gravity = Gravity.CENTER
            id = UNIT_SPINNER_ID
        }

        /**SWITCH LOGIC*/
        switch.setOnCheckedChangeListener { _: CompoundButton, isChecked: Boolean ->
            if (isChecked) {
                chooseProductTextView.text = resources.getString(R.string.choose_half_product)
                switch.text = "Switch to products"
                productSpinner.adapter = halfProductAdapter
            } else {
                chooseProductTextView.text = resources.getString(R.string.choose_product)
                switch.text = "Switch to half products"
                productSpinner.adapter = productAdapter

            }
        }

        /**OBSERVING 'LIVEDATA' FROM ADDVIEWMODEL
         *  WHICH OBSERVES 'LIVEDATA' IN REPOSITORY
         *  WHICH OBSERVES 'LIVEDATA' FROM DAO*/

        viewModel.readAllProductData.observe(viewLifecycleOwner, Observer { products ->
            productAdapter.clear()
            products.forEach { product ->
                productAdapter.add(product.name)
                productAdapter.notifyDataSetChanged()
            }
        })

        viewModel.readAllDishData.observe(viewLifecycleOwner, Observer { dishes ->
            dishesAdapter.clear()
            dishes.forEach { dish ->
                dishesAdapter.add(dish.name)
                dishesAdapter.notifyDataSetChanged()
            }
        })

        /** BUTTON LOGIC*/
        addProductToDishBtn.setOnClickListener {

            if (weightOfAddedProduct.text.isNullOrEmpty() || weightOfAddedProduct.text.toString() == ".") {
                showToast(message = "You can't add product without weight.")
            }
            else if(viewModel.readAllDishData.value.isNullOrEmpty()){
                showToast(message = "You must pick a dish.")
            }
            else if (!switch.isChecked) {
                if(viewModel.readAllProductData.value.isNullOrEmpty()){
                    showToast(message = "You must pick a product.")
                    return@setOnClickListener
                }
                val chosenDish = viewModel.readAllDishData.value?.get(dishPosition!!)
                val weight = weightOfAddedProduct.text.toString().toDouble()
                val chosenProduct = viewModel.readAllProductData.value?.get(productPosition!!)
                viewModel.addProductToDish(
                    ProductIncluded(
                        0,
                        chosenProduct!!,
                        chosenDish!!.dishId,
                        chosenDish,
                        chosenProduct.productId,
                        weight,
                        chosenUnit
                    )
                )
                weightOfAddedProduct.text.clear()
                showToast(message = "${viewModel.readAllProductData.value?.get(productPosition!!)?.name} added.")
            } else {
                if(viewModel.getHalfProducts().value.isNullOrEmpty()){
                    showToast(message = "You must pick a product.")
                    return@setOnClickListener
                }
                val chosenDish = viewModel.readAllDishData.value?.get(dishPosition!!)
                val weight = weightOfAddedProduct.text.toString().toDouble()
                viewModel.getHalfProducts().observe(viewLifecycleOwner,  { halfProduct ->
                    halfProductToAdd = halfProduct[productPosition!!]
                })

                viewModel.addHalfProductIncludedInDish(
                    HalfProductIncludedInDish(
                        0,
                        chosenDish!!,
                        chosenDish.dishId,
                        halfProductToAdd,
                        halfProductToAdd.halfProductId,
                        weight,
                        chosenUnit
                    )
                )
                weightOfAddedProduct.text.clear()
                showToast(message = "${halfProductToAdd.name} added." )
            }
        }

        dialog?.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        return view
    }

    companion object {
        fun newInstance(): AddProductToDish =
            AddProductToDish()
        const val TAG = "AddProductToDish"
    }

    private fun showToast(
        context: FragmentActivity? = activity,
        message: String,
        duration: Int = Toast.LENGTH_SHORT
    ) {
        Toast.makeText(context, message, duration).show()
    }

    private fun isOpenedFromDishAdapter():Boolean{
      return this.tag == "DishAdapter"
    }

}


