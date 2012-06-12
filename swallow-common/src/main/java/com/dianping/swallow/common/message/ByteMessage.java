package com.dianping.swallow.common.message;

public class ByteMessage extends AbstractMessage<byte[]> {

   private static final long serialVersionUID = -8995095503382707220L;

   private byte[]            content;

   public ByteMessage() {
      this.setContentType(Message.ContentType.BytesMessage);
   }

   @Override
   public byte[] getContent() {
      return content;
   }

   public void setContent(byte[] content) {
      this.content = content;
   }

}