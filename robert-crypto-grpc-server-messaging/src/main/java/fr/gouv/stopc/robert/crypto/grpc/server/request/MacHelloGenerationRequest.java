// Generated by the protocol buffer compiler.  DO NOT EDIT!
// source: crypto_service.proto

package fr.gouv.stopc.robert.crypto.grpc.server.request;

import fr.gouv.stopc.robert.crypto.grpc.server.service.CryptoGrpcService;

/**
 * Protobuf type {@code robert.server.crypto.MacHelloGenerationRequest}
 */
public  final class MacHelloGenerationRequest extends
    com.google.protobuf.GeneratedMessageV3 implements
    // @@protoc_insertion_point(message_implements:robert.server.crypto.MacHelloGenerationRequest)
    MacHelloGenerationRequestOrBuilder {
private static final long serialVersionUID = 0L;
  // Use MacHelloGenerationRequest.newBuilder() to construct.
  private MacHelloGenerationRequest(com.google.protobuf.GeneratedMessageV3.Builder<?> builder) {
    super(builder);
  }
  private MacHelloGenerationRequest() {
    ka_ = com.google.protobuf.ByteString.EMPTY;
    helloMessage_ = com.google.protobuf.ByteString.EMPTY;
  }

  @java.lang.Override
  @SuppressWarnings({"unused"})
  protected java.lang.Object newInstance(
      UnusedPrivateParameter unused) {
    return new MacHelloGenerationRequest();
  }

  @java.lang.Override
  public final com.google.protobuf.UnknownFieldSet
  getUnknownFields() {
    return this.unknownFields;
  }
  private MacHelloGenerationRequest(
      com.google.protobuf.CodedInputStream input,
      com.google.protobuf.ExtensionRegistryLite extensionRegistry)
      throws com.google.protobuf.InvalidProtocolBufferException {
    this();
    if (extensionRegistry == null) {
      throw new java.lang.NullPointerException();
    }
    com.google.protobuf.UnknownFieldSet.Builder unknownFields =
        com.google.protobuf.UnknownFieldSet.newBuilder();
    try {
      boolean done = false;
      while (!done) {
        int tag = input.readTag();
        switch (tag) {
          case 0:
            done = true;
            break;
          case 10: {

            ka_ = input.readBytes();
            break;
          }
          case 18: {

            helloMessage_ = input.readBytes();
            break;
          }
          default: {
            if (!parseUnknownField(
                input, unknownFields, extensionRegistry, tag)) {
              done = true;
            }
            break;
          }
        }
      }
    } catch (com.google.protobuf.InvalidProtocolBufferException e) {
      throw e.setUnfinishedMessage(this);
    } catch (java.io.IOException e) {
      throw new com.google.protobuf.InvalidProtocolBufferException(
          e).setUnfinishedMessage(this);
    } finally {
      this.unknownFields = unknownFields.build();
      makeExtensionsImmutable();
    }
  }
  public static final com.google.protobuf.Descriptors.Descriptor
      getDescriptor() {
    return CryptoGrpcService.internal_static_robert_server_crypto_MacHelloGenerationRequest_descriptor;
  }

  @java.lang.Override
  protected com.google.protobuf.GeneratedMessageV3.FieldAccessorTable
      internalGetFieldAccessorTable() {
    return CryptoGrpcService.internal_static_robert_server_crypto_MacHelloGenerationRequest_fieldAccessorTable
        .ensureFieldAccessorsInitialized(
            MacHelloGenerationRequest.class, MacHelloGenerationRequest.Builder.class);
  }

  public static final int KA_FIELD_NUMBER = 1;
  private com.google.protobuf.ByteString ka_;
  /**
   * <pre>
   * byte[] KA;
   * byte[] helloMessage;
   * </pre>
   *
   * <code>bytes ka = 1;</code>
   * @return The ka.
   */
  @Override
public com.google.protobuf.ByteString getKa() {
    return ka_;
  }

  public static final int HELLOMESSAGE_FIELD_NUMBER = 2;
  private com.google.protobuf.ByteString helloMessage_;
  /**
   * <code>bytes helloMessage = 2;</code>
   * @return The helloMessage.
   */
  @Override
public com.google.protobuf.ByteString getHelloMessage() {
    return helloMessage_;
  }

  private byte memoizedIsInitialized = -1;
  @java.lang.Override
  public final boolean isInitialized() {
    byte isInitialized = memoizedIsInitialized;
    if (isInitialized == 1) return true;
    if (isInitialized == 0) return false;

    memoizedIsInitialized = 1;
    return true;
  }

  @java.lang.Override
  public void writeTo(com.google.protobuf.CodedOutputStream output)
                      throws java.io.IOException {
    if (!ka_.isEmpty()) {
      output.writeBytes(1, ka_);
    }
    if (!helloMessage_.isEmpty()) {
      output.writeBytes(2, helloMessage_);
    }
    unknownFields.writeTo(output);
  }

  @java.lang.Override
  public int getSerializedSize() {
    int size = memoizedSize;
    if (size != -1) return size;

    size = 0;
    if (!ka_.isEmpty()) {
      size += com.google.protobuf.CodedOutputStream
        .computeBytesSize(1, ka_);
    }
    if (!helloMessage_.isEmpty()) {
      size += com.google.protobuf.CodedOutputStream
        .computeBytesSize(2, helloMessage_);
    }
    size += unknownFields.getSerializedSize();
    memoizedSize = size;
    return size;
  }

  @java.lang.Override
  public boolean equals(final java.lang.Object obj) {
    if (obj == this) {
     return true;
    }
    if (!(obj instanceof MacHelloGenerationRequest)) {
      return super.equals(obj);
    }
    MacHelloGenerationRequest other = (MacHelloGenerationRequest) obj;

    if (!getKa()
        .equals(other.getKa())) return false;
    if (!getHelloMessage()
        .equals(other.getHelloMessage())) return false;
    if (!unknownFields.equals(other.unknownFields)) return false;
    return true;
  }

  @java.lang.Override
  public int hashCode() {
    if (memoizedHashCode != 0) {
      return memoizedHashCode;
    }
    int hash = 41;
    hash = (19 * hash) + getDescriptor().hashCode();
    hash = (37 * hash) + KA_FIELD_NUMBER;
    hash = (53 * hash) + getKa().hashCode();
    hash = (37 * hash) + HELLOMESSAGE_FIELD_NUMBER;
    hash = (53 * hash) + getHelloMessage().hashCode();
    hash = (29 * hash) + unknownFields.hashCode();
    memoizedHashCode = hash;
    return hash;
  }

  public static MacHelloGenerationRequest parseFrom(
      java.nio.ByteBuffer data)
      throws com.google.protobuf.InvalidProtocolBufferException {
    return PARSER.parseFrom(data);
  }
  public static MacHelloGenerationRequest parseFrom(
      java.nio.ByteBuffer data,
      com.google.protobuf.ExtensionRegistryLite extensionRegistry)
      throws com.google.protobuf.InvalidProtocolBufferException {
    return PARSER.parseFrom(data, extensionRegistry);
  }
  public static MacHelloGenerationRequest parseFrom(
      com.google.protobuf.ByteString data)
      throws com.google.protobuf.InvalidProtocolBufferException {
    return PARSER.parseFrom(data);
  }
  public static MacHelloGenerationRequest parseFrom(
      com.google.protobuf.ByteString data,
      com.google.protobuf.ExtensionRegistryLite extensionRegistry)
      throws com.google.protobuf.InvalidProtocolBufferException {
    return PARSER.parseFrom(data, extensionRegistry);
  }
  public static MacHelloGenerationRequest parseFrom(byte[] data)
      throws com.google.protobuf.InvalidProtocolBufferException {
    return PARSER.parseFrom(data);
  }
  public static MacHelloGenerationRequest parseFrom(
      byte[] data,
      com.google.protobuf.ExtensionRegistryLite extensionRegistry)
      throws com.google.protobuf.InvalidProtocolBufferException {
    return PARSER.parseFrom(data, extensionRegistry);
  }
  public static MacHelloGenerationRequest parseFrom(java.io.InputStream input)
      throws java.io.IOException {
    return com.google.protobuf.GeneratedMessageV3
        .parseWithIOException(PARSER, input);
  }
  public static MacHelloGenerationRequest parseFrom(
      java.io.InputStream input,
      com.google.protobuf.ExtensionRegistryLite extensionRegistry)
      throws java.io.IOException {
    return com.google.protobuf.GeneratedMessageV3
        .parseWithIOException(PARSER, input, extensionRegistry);
  }
  public static MacHelloGenerationRequest parseDelimitedFrom(java.io.InputStream input)
      throws java.io.IOException {
    return com.google.protobuf.GeneratedMessageV3
        .parseDelimitedWithIOException(PARSER, input);
  }
  public static MacHelloGenerationRequest parseDelimitedFrom(
      java.io.InputStream input,
      com.google.protobuf.ExtensionRegistryLite extensionRegistry)
      throws java.io.IOException {
    return com.google.protobuf.GeneratedMessageV3
        .parseDelimitedWithIOException(PARSER, input, extensionRegistry);
  }
  public static MacHelloGenerationRequest parseFrom(
      com.google.protobuf.CodedInputStream input)
      throws java.io.IOException {
    return com.google.protobuf.GeneratedMessageV3
        .parseWithIOException(PARSER, input);
  }
  public static MacHelloGenerationRequest parseFrom(
      com.google.protobuf.CodedInputStream input,
      com.google.protobuf.ExtensionRegistryLite extensionRegistry)
      throws java.io.IOException {
    return com.google.protobuf.GeneratedMessageV3
        .parseWithIOException(PARSER, input, extensionRegistry);
  }

  @java.lang.Override
  public Builder newBuilderForType() { return newBuilder(); }
  public static Builder newBuilder() {
    return DEFAULT_INSTANCE.toBuilder();
  }
  public static Builder newBuilder(MacHelloGenerationRequest prototype) {
    return DEFAULT_INSTANCE.toBuilder().mergeFrom(prototype);
  }
  @java.lang.Override
  public Builder toBuilder() {
    return this == DEFAULT_INSTANCE
        ? new Builder() : new Builder().mergeFrom(this);
  }

  @java.lang.Override
  protected Builder newBuilderForType(
      com.google.protobuf.GeneratedMessageV3.BuilderParent parent) {
    Builder builder = new Builder(parent);
    return builder;
  }
  /**
   * Protobuf type {@code robert.server.crypto.MacHelloGenerationRequest}
   */
  public static final class Builder extends
      com.google.protobuf.GeneratedMessageV3.Builder<Builder> implements
      // @@protoc_insertion_point(builder_implements:robert.server.crypto.MacHelloGenerationRequest)
      MacHelloGenerationRequestOrBuilder {
    public static final com.google.protobuf.Descriptors.Descriptor
        getDescriptor() {
      return CryptoGrpcService.internal_static_robert_server_crypto_MacHelloGenerationRequest_descriptor;
    }

    @java.lang.Override
    protected com.google.protobuf.GeneratedMessageV3.FieldAccessorTable
        internalGetFieldAccessorTable() {
      return CryptoGrpcService.internal_static_robert_server_crypto_MacHelloGenerationRequest_fieldAccessorTable
          .ensureFieldAccessorsInitialized(
              MacHelloGenerationRequest.class, MacHelloGenerationRequest.Builder.class);
    }

    // Construct using MacHelloGenerationRequest.newBuilder()
    private Builder() {
      maybeForceBuilderInitialization();
    }

    private Builder(
        com.google.protobuf.GeneratedMessageV3.BuilderParent parent) {
      super(parent);
      maybeForceBuilderInitialization();
    }
    private void maybeForceBuilderInitialization() {
      if (com.google.protobuf.GeneratedMessageV3
              .alwaysUseFieldBuilders) {
      }
    }
    @java.lang.Override
    public Builder clear() {
      super.clear();
      ka_ = com.google.protobuf.ByteString.EMPTY;

      helloMessage_ = com.google.protobuf.ByteString.EMPTY;

      return this;
    }

    @java.lang.Override
    public com.google.protobuf.Descriptors.Descriptor
        getDescriptorForType() {
      return CryptoGrpcService.internal_static_robert_server_crypto_MacHelloGenerationRequest_descriptor;
    }

    @java.lang.Override
    public MacHelloGenerationRequest getDefaultInstanceForType() {
      return MacHelloGenerationRequest.getDefaultInstance();
    }

    @java.lang.Override
    public MacHelloGenerationRequest build() {
      MacHelloGenerationRequest result = buildPartial();
      if (!result.isInitialized()) {
        throw newUninitializedMessageException(result);
      }
      return result;
    }

    @java.lang.Override
    public MacHelloGenerationRequest buildPartial() {
      MacHelloGenerationRequest result = new MacHelloGenerationRequest(this);
      result.ka_ = ka_;
      result.helloMessage_ = helloMessage_;
      onBuilt();
      return result;
    }

    @java.lang.Override
    public Builder clone() {
      return super.clone();
    }
    @java.lang.Override
    public Builder setField(
        com.google.protobuf.Descriptors.FieldDescriptor field,
        java.lang.Object value) {
      return super.setField(field, value);
    }
    @java.lang.Override
    public Builder clearField(
        com.google.protobuf.Descriptors.FieldDescriptor field) {
      return super.clearField(field);
    }
    @java.lang.Override
    public Builder clearOneof(
        com.google.protobuf.Descriptors.OneofDescriptor oneof) {
      return super.clearOneof(oneof);
    }
    @java.lang.Override
    public Builder setRepeatedField(
        com.google.protobuf.Descriptors.FieldDescriptor field,
        int index, java.lang.Object value) {
      return super.setRepeatedField(field, index, value);
    }
    @java.lang.Override
    public Builder addRepeatedField(
        com.google.protobuf.Descriptors.FieldDescriptor field,
        java.lang.Object value) {
      return super.addRepeatedField(field, value);
    }
    @java.lang.Override
    public Builder mergeFrom(com.google.protobuf.Message other) {
      if (other instanceof MacHelloGenerationRequest) {
        return mergeFrom((MacHelloGenerationRequest)other);
      } else {
        super.mergeFrom(other);
        return this;
      }
    }

    public Builder mergeFrom(MacHelloGenerationRequest other) {
      if (other == MacHelloGenerationRequest.getDefaultInstance()) return this;
      if (other.getKa() != com.google.protobuf.ByteString.EMPTY) {
        setKa(other.getKa());
      }
      if (other.getHelloMessage() != com.google.protobuf.ByteString.EMPTY) {
        setHelloMessage(other.getHelloMessage());
      }
      this.mergeUnknownFields(other.unknownFields);
      onChanged();
      return this;
    }

    @java.lang.Override
    public final boolean isInitialized() {
      return true;
    }

    @java.lang.Override
    public Builder mergeFrom(
        com.google.protobuf.CodedInputStream input,
        com.google.protobuf.ExtensionRegistryLite extensionRegistry)
        throws java.io.IOException {
      MacHelloGenerationRequest parsedMessage = null;
      try {
        parsedMessage = PARSER.parsePartialFrom(input, extensionRegistry);
      } catch (com.google.protobuf.InvalidProtocolBufferException e) {
        parsedMessage = (MacHelloGenerationRequest) e.getUnfinishedMessage();
        throw e.unwrapIOException();
      } finally {
        if (parsedMessage != null) {
          mergeFrom(parsedMessage);
        }
      }
      return this;
    }

    private com.google.protobuf.ByteString ka_ = com.google.protobuf.ByteString.EMPTY;
    /**
     * <pre>
     * byte[] KA;
     * byte[] helloMessage;
     * </pre>
     *
     * <code>bytes ka = 1;</code>
     * @return The ka.
     */
    @Override
	public com.google.protobuf.ByteString getKa() {
      return ka_;
    }
    /**
     * <pre>
     * byte[] KA;
     * byte[] helloMessage;
     * </pre>
     *
     * <code>bytes ka = 1;</code>
     * @param value The ka to set.
     * @return This builder for chaining.
     */
    public Builder setKa(com.google.protobuf.ByteString value) {
      if (value == null) {
    throw new NullPointerException();
  }
  
      ka_ = value;
      onChanged();
      return this;
    }
    /**
     * <pre>
     * byte[] KA;
     * byte[] helloMessage;
     * </pre>
     *
     * <code>bytes ka = 1;</code>
     * @return This builder for chaining.
     */
    public Builder clearKa() {
      
      ka_ = getDefaultInstance().getKa();
      onChanged();
      return this;
    }

    private com.google.protobuf.ByteString helloMessage_ = com.google.protobuf.ByteString.EMPTY;
    /**
     * <code>bytes helloMessage = 2;</code>
     * @return The helloMessage.
     */
    @Override
	public com.google.protobuf.ByteString getHelloMessage() {
      return helloMessage_;
    }
    /**
     * <code>bytes helloMessage = 2;</code>
     * @param value The helloMessage to set.
     * @return This builder for chaining.
     */
    public Builder setHelloMessage(com.google.protobuf.ByteString value) {
      if (value == null) {
    throw new NullPointerException();
  }
  
      helloMessage_ = value;
      onChanged();
      return this;
    }
    /**
     * <code>bytes helloMessage = 2;</code>
     * @return This builder for chaining.
     */
    public Builder clearHelloMessage() {
      
      helloMessage_ = getDefaultInstance().getHelloMessage();
      onChanged();
      return this;
    }
    @java.lang.Override
    public final Builder setUnknownFields(
        final com.google.protobuf.UnknownFieldSet unknownFields) {
      return super.setUnknownFields(unknownFields);
    }

    @java.lang.Override
    public final Builder mergeUnknownFields(
        final com.google.protobuf.UnknownFieldSet unknownFields) {
      return super.mergeUnknownFields(unknownFields);
    }


    // @@protoc_insertion_point(builder_scope:robert.server.crypto.MacHelloGenerationRequest)
  }

  // @@protoc_insertion_point(class_scope:robert.server.crypto.MacHelloGenerationRequest)
  private static final MacHelloGenerationRequest DEFAULT_INSTANCE;
  static {
    DEFAULT_INSTANCE = new MacHelloGenerationRequest();
  }

  public static MacHelloGenerationRequest getDefaultInstance() {
    return DEFAULT_INSTANCE;
  }

  private static final com.google.protobuf.Parser<MacHelloGenerationRequest>
      PARSER = new com.google.protobuf.AbstractParser<MacHelloGenerationRequest>() {
    @java.lang.Override
    public MacHelloGenerationRequest parsePartialFrom(
        com.google.protobuf.CodedInputStream input,
        com.google.protobuf.ExtensionRegistryLite extensionRegistry)
        throws com.google.protobuf.InvalidProtocolBufferException {
      return new MacHelloGenerationRequest(input, extensionRegistry);
    }
  };

  public static com.google.protobuf.Parser<MacHelloGenerationRequest> parser() {
    return PARSER;
  }

  @java.lang.Override
  public com.google.protobuf.Parser<MacHelloGenerationRequest> getParserForType() {
    return PARSER;
  }

  @java.lang.Override
  public MacHelloGenerationRequest getDefaultInstanceForType() {
    return DEFAULT_INSTANCE;
  }

}

