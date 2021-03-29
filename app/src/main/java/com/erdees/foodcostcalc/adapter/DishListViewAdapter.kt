package com.erdees.foodcostcalc.adapter

import android.annotation.SuppressLint
import android.app.Activity
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.TextView
import androidx.lifecycle.LifecycleOwner
import com.erdees.foodcostcalc.R
import com.erdees.foodcostcalc.calculatePrice
import com.erdees.foodcostcalc.formatPrice
import com.erdees.foodcostcalc.model.GrandDish
import com.erdees.foodcostcalc.unitAbbreviation
import com.erdees.foodcostcalc.viewmodel.HalfProductsViewModel
import com.erdees.foodcostcalc.viewmodel.adaptersViewModel.DishListViewAdapterViewModel

class DishListViewAdapter(private val context: Activity,
                          private val grandDish: GrandDish,
                          private val viewModel: DishListViewAdapterViewModel,
                          private val viewLifecycleOwner: LifecycleOwner)
    : ArrayAdapter<Any>(context, R.layout.listview_dish_row,
    grandDish.productsIncluded
        + grandDish.halfProducts) {


    @SuppressLint("ViewHolder")
    override fun getView(position: Int, view: View?, parent: ViewGroup): View {

        val inflater = context.layoutInflater
        val rowView = inflater.inflate(R.layout.listview_dish_row,
            parent, false)

        val productNameText = rowView.findViewById<TextView>(R.id.product_name_in_dish_row)
        val productWeightText = rowView.findViewById<TextView>(R.id.product_weight_in_dish_row)
        val productPriceText = rowView.findViewById<TextView>(R.id.product_price_in_dish_row)
        val productUnit = rowView.findViewById<TextView>(R.id.product_weight_unit_in_dish_row)

        /**To populate rows with dishWithProduct products included */
        if (position < grandDish.productsIncluded.size ) {
            productNameText.text = grandDish.productsIncluded[position].productIncluded.name
            productWeightText.text = grandDish.productsIncluded[position].formattedWeight
            productPriceText.text = grandDish.productsIncluded[position].finalFormatPriceOfProduct
            productUnit.text = unitAbbreviation(grandDish.productsIncluded[position].weightUnit)
        }

        /**To populate rest with halfProducts*/
        else if (position >= grandDish.productsIncluded.size)  {
            var thisPosition = position - grandDish.productsIncluded.size // to start counting position from new list
            productNameText.text = grandDish.halfProducts[thisPosition].halfProduct.name
            productWeightText.text = grandDish.halfProducts[thisPosition].weight.toString()


            viewModel
                .getCertainHalfProductWithProductsIncluded(grandDish.halfProducts[thisPosition].halfProductOwnerId)
                .observe(viewLifecycleOwner,
                    { productPriceText.text =
                        formatPrice(
                            calculatePrice(it.pricePerUnit(),grandDish.halfProducts[thisPosition].weight,
                                it.halfProduct.halfProductUnit, grandDish.halfProducts[thisPosition].unit)
                            )
                    })

            productUnit.text = unitAbbreviation(grandDish.halfProducts[thisPosition].unit)
        }
        return rowView

    }

    }