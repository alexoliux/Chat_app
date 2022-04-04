package mirea.buryakov.chatapp;

import android.annotation.SuppressLint;
import android.content.Context;
import android.preference.PreferenceManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.squareup.picasso.Picasso;

import java.util.ArrayList;
import java.util.List;

public class MessagesAdapter extends RecyclerView.Adapter<MessagesAdapter.MessageViewHolder> {

    private static final int TYPE_MY_MESSAGE = 0;
    private static final int TYPE_OTHER_MESSAGE = 1;

    private List<Message> messages;
    private Context context;

    public MessagesAdapter(Context context) {
        messages = new ArrayList<>();
        this.context = context;
    }

    @SuppressLint("NotifyDataSetChanged")
    public void setMessages(List<Message> messages) {
        this.messages = messages;
        notifyDataSetChanged();
    }

    public List<Message> getMessages() {
        return messages;
    }

    @NonNull
    @Override
    public MessageViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view;
        if (viewType == TYPE_MY_MESSAGE) {
            view = LayoutInflater.from(parent.getContext()).inflate(R.layout.layout_item_my_message, parent, false);
        } else {
            view = LayoutInflater.from(parent.getContext()).inflate(R.layout.layout_item_other_message, parent, false);
        }
        return new MessageViewHolder(view);
    }

    @Override
    public int getItemViewType(int position) {
        Message message = messages.get(position);
        String  author = message.getAuthor();
        if (author != null && author.equals(PreferenceManager.getDefaultSharedPreferences(context).getString("author", "Anonim"))) {
            return TYPE_MY_MESSAGE;
        } else {
            return TYPE_OTHER_MESSAGE;
        }
    }

    @Override
    public void onBindViewHolder(@NonNull MessageViewHolder holder, int position) {
        Message message = messages.get(position);
        String author = message.getAuthor();
        String textOfMessage = message.getTextOfMessage();
        String urlToImage = message.getImageUrl();
        holder.textViewAuthor.setText(author);
        if (urlToImage == null || urlToImage.isEmpty()) {
            holder.imageViewImage.setVisibility(View.GONE);
        } else {
            holder.imageViewImage.setVisibility(View.VISIBLE);
        }
        if (textOfMessage != null && !textOfMessage.isEmpty()) {
            holder.textViewTextOfMessage.setVisibility(View.VISIBLE);
            holder.textViewTextOfMessage.setText(textOfMessage);
        } else {
            holder.textViewTextOfMessage.setVisibility(View.GONE);
        }
        if (urlToImage != null) {
            Picasso.get().load(urlToImage).into(holder.imageViewImage);
        }
    }

    @Override
    public int getItemCount() {
        return messages.size();
    }

    class MessageViewHolder extends RecyclerView.ViewHolder {

        private TextView textViewAuthor;
        private TextView textViewTextOfMessage;
        private ImageView imageViewImage;

        public MessageViewHolder(@NonNull View itemView) {
            super(itemView);
            textViewAuthor = itemView.findViewById(R.id.textViewAuthor);
            textViewTextOfMessage = itemView.findViewById(R.id.textViewOfMessage);
            imageViewImage = itemView.findViewById(R.id.imageViewImage);
        }
    }
}
