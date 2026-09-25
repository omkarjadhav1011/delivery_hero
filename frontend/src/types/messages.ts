// Real-time messages, mirroring document 11 section 8. Every server message carries type and serverTime (DEC-162).
// TODO(EN-04): the discriminated union of every message type, checked against contracts/

export type Envelope = {
  type: string;
  serverTime: number;
};
