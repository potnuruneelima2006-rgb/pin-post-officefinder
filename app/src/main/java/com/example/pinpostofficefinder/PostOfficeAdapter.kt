package com.example.pinpostofficefinder

import android.content.Intent
import android.net.Uri
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.pinpostofficefinder.databinding.ItemPostOfficeBinding

class PostOfficeAdapter(
    private val list: List<PostOffice>,
    private val onSave: (PostOffice) -> Unit
) : RecyclerView.Adapter<PostOfficeAdapter.ViewHolder>() {

    class ViewHolder(
        val binding: ItemPostOfficeBinding
    ) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): ViewHolder {

        val binding = ItemPostOfficeBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )

        return ViewHolder(binding)
    }

    override fun onBindViewHolder(
        holder: ViewHolder,
        position: Int
    ) {

        val office = list[position]

        holder.binding.tvName.text =
            "Name: ${office.Name}"

        holder.binding.tvBranchType.text =
            "Branch Type: ${office.BranchType}"

        holder.binding.tvDelivery.text =
            "Delivery: ${office.DeliveryStatus}"

        holder.binding.tvDistrict.text =
            "District: ${office.District}"

        holder.binding.tvState.text =
            "State: ${office.State}"

        holder.binding.btnSave.setOnClickListener {
            onSave(office)
        }

        holder.binding.btnMap.setOnClickListener {

            val query = Uri.parse(
                "geo:0,0?q=${Uri.encode(office.Name + ", " + office.District)}"
            )

            val intent = Intent(
                Intent.ACTION_VIEW,
                query
            )

            holder.itemView.context.startActivity(intent)
        }
    }

    override fun getItemCount(): Int {
        return list.size
    }
}
