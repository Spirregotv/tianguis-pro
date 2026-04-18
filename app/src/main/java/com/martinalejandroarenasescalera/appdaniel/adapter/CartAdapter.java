package com.martinalejandroarenasescalera.appdaniel.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.martinalejandroarenasescalera.appdaniel.R;
import com.martinalejandroarenasescalera.appdaniel.model.CartItem;

import java.util.List;

/**
 * CartAdapter – RecyclerView para el carrito de compras.
 * Permite modificar la cantidad (+/-) y eliminar ítems.
 */
public class CartAdapter extends RecyclerView.Adapter<CartAdapter.CartViewHolder> {

    public interface OnCartChangeListener {
        void onQuantityChanged();
        void onItemRemoved(int position);
    }

    private final List<CartItem> items;
    private final OnCartChangeListener listener;

    public CartAdapter(List<CartItem> items, OnCartChangeListener listener) {
        this.items = items;
        this.listener = listener;
    }

    @NonNull
    @Override
    public CartViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_cart, parent, false);
        return new CartViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull CartViewHolder holder, int position) {
        holder.bind(items.get(position), position);
    }

    @Override
    public int getItemCount() { return items.size(); }

    class CartViewHolder extends RecyclerView.ViewHolder {

        private final ImageView imgProduct;
        private final TextView tvName, tvPrice, tvQuantity, tvSubtotal;
        private final ImageButton btnIncrease, btnDecrease, btnRemove;

        CartViewHolder(@NonNull View itemView) {
            super(itemView);
            imgProduct = itemView.findViewById(R.id.imgProduct);
            tvName = itemView.findViewById(R.id.tvProductName);
            tvPrice = itemView.findViewById(R.id.tvProductPrice);
            tvQuantity = itemView.findViewById(R.id.tvQuantity);
            tvSubtotal = itemView.findViewById(R.id.tvSubtotal);
            btnIncrease = itemView.findViewById(R.id.btnIncrease);
            btnDecrease = itemView.findViewById(R.id.btnDecrease);
            btnRemove = itemView.findViewById(R.id.btnRemove);
        }

        void bind(CartItem item, int position) {
            tvName.setText(item.getProduct().getName());
            tvPrice.setText(String.format("$%.2f c/u", item.getProduct().getPrice()));
            tvQuantity.setText(String.valueOf(item.getQuantity()));
            tvSubtotal.setText(String.format("$%.2f", item.getSubtotal()));

            if (item.getProduct().getImageUrl() != null && !item.getProduct().getImageUrl().isEmpty()) {
                Glide.with(itemView.getContext())
                        .load(item.getProduct().getImageUrl())
                        .placeholder(R.drawable.ic_product_placeholder)
                        .centerCrop()
                        .into(imgProduct);
            } else {
                imgProduct.setImageResource(R.drawable.ic_product_placeholder);
            }

            btnIncrease.setOnClickListener(v -> {
                if (item.increment()) {
                    tvQuantity.setText(String.valueOf(item.getQuantity()));
                    tvSubtotal.setText(String.format("$%.2f", item.getSubtotal()));
                    listener.onQuantityChanged();
                }
            });

            btnDecrease.setOnClickListener(v -> {
                if (item.decrement()) {
                    tvQuantity.setText(String.valueOf(item.getQuantity()));
                    tvSubtotal.setText(String.format("$%.2f", item.getSubtotal()));
                    listener.onQuantityChanged();
                }
            });

            btnRemove.setOnClickListener(v -> {
                items.remove(position);
                notifyItemRemoved(position);
                notifyItemRangeChanged(position, items.size());
                listener.onItemRemoved(position);
            });
        }
    }
}
