// Copyright 2023 ETH Zurich
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//   http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

package de.ovgu.netsys.scion_ip_translator.tra;

import org.scion.jpan.internal.ByteUtil;

import java.nio.ByteBuffer;

public class ScionHdrParserMod {
    // From org.scion.jpan.internal.ScionHeaderParser
    private static int calcLen(int pathHeaderLength, int sl, int dl) {
        // Common header
        int len = 12;

        // Address header
        len += 16;
        len += (dl + 1) * 4;
        len += (sl + 1) * 4;

        // Path header
        len += pathHeaderLength;
        return len;
    }

    // From org.scion.jpan.internal.ScionHeaderParser
    // Modified to take arbitrary values for hdrType as JPAN currently does not define an enum
    // constant for TCP.
    public static void write(
            ByteBuffer data,
            int userPacketLength,
            int pathHeaderLength,
            long srcIsdAs,
            byte[] srcAddress,
            long dstIsdAs,
            byte[] dstAddress,
            int hdrType,
            int trafficClass) {
        int sl = srcAddress.length / 4 - 1;
        int dl = dstAddress.length / 4 - 1;

        int i0 = 0;
        int i1 = 0;
        int i2 = 0;
        i0 = ByteUtil.writeInt(i0, 0, 4, 0); // version = 0
        i0 = ByteUtil.writeInt(i0, 4, 8, trafficClass); // TrafficClass = 0
        i0 = ByteUtil.writeInt(i0, 12, 20, 1); // FlowID = 1
        data.putInt(i0);
        i1 = ByteUtil.writeInt(i1, 0, 8, hdrType); // NextHdr = 17 is for UDP OverlayHeader
        int newHdrLen = (calcLen(pathHeaderLength, sl, dl) - 1) / 4 + 1;
        i1 = ByteUtil.writeInt(i1, 8, 8, newHdrLen); // HdrLen = bytes/4
        i1 = ByteUtil.writeInt(i1, 16, 16, userPacketLength); // PayloadLen (+ overlay!)
        data.putInt(i1);
        i2 = ByteUtil.writeInt(i2, 0, 8, pathHeaderLength > 0 ? 1 : 0); // PathType : SCION = 1
        i2 = ByteUtil.writeInt(i2, 8, 2, 0); // DT
        i2 = ByteUtil.writeInt(i2, 10, 2, dl); // DL
        i2 = ByteUtil.writeInt(i2, 12, 2, 0); // ST
        i2 = ByteUtil.writeInt(i2, 14, 2, sl); // SL
        i2 = ByteUtil.writeInt(i2, 16, 16, 0x0); // RSV
        data.putInt(i2);

        // Address header
        data.putLong(dstIsdAs);
        data.putLong(srcIsdAs);

        // HostAddr
        data.put(dstAddress);
        data.put(srcAddress);
    }
}
