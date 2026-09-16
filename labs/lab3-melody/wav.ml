(* Provided back-end: timed events -> .wav file. Voices are summed, then normalised. *)
let rate = 44100.
let pi = 4.0 *. atan 1.0

let write filename (events : Melody.event list) =
  let total = List.fold_left (fun m (e : Melody.event) -> Float.max m (e.at +. e.secs)) 0. events in
  let buf = Array.make (int_of_float (total *. rate) + 1) 0. in
  List.iter (fun (e : Melody.event) ->
    let s0 = int_of_float (e.at *. rate) and len = int_of_float (e.secs *. rate) in
    for i = 0 to len - 1 do
      let fade = 300 in                                   (* short envelope: no clicks *)
      let env = Float.min 1. (Float.min (float_of_int i /. float_of_int fade)
                                        (float_of_int (len - i) /. float_of_int fade)) in
      buf.(s0 + i) <- buf.(s0 + i) +. env *. sin (2. *. pi *. e.hz *. float_of_int i /. rate)
    done) events;
  let peak = Array.fold_left (fun m x -> Float.max m (Float.abs x)) 1e-9 buf in
  let oc = open_out_bin filename in
  let le32 n = for k = 0 to 3 do output_byte oc ((n lsr (8*k)) land 0xFF) done in
  let le16 n = for k = 0 to 1 do output_byte oc ((n lsr (8*k)) land 0xFF) done in
  let n = Array.length buf in
  output_string oc "RIFF"; le32 (36 + n*2); output_string oc "WAVEfmt ";
  le32 16; le16 1; le16 1; le32 44100; le32 (44100*2); le16 2; le16 16;
  output_string oc "data"; le32 (n*2);
  Array.iter (fun x -> le16 (int_of_float (x /. peak *. 28000.) land 0xFFFF)) buf;
  close_out oc
