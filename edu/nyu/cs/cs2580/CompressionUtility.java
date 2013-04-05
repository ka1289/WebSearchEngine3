package edu.nyu.cs.cs2580;

public class CompressionUtility {

	public static String encodeByteAlign(int number) {
		String _toBinary = Integer.toBinaryString(number);
		String appendZero = appendZeros(_toBinary);
		String additionZero = appendAdditionalZeroOrOne(appendZero);
		return binaryToHex(additionZero);
	}

	public static String convertToBinary(int num) {
		return Integer.toBinaryString(num);
	}

	public static String appendZeros(String binaryNumber) {
		int length = binaryNumber.length();
		if (length % 7 == 0) {
			return binaryNumber;
		} else {
			int remainingDiff = (length % 7);
			int diff = 7 - remainingDiff;
			StringBuffer buffer = new StringBuffer();
			for (int i = 0; i < diff; i++) {
				buffer.append(0);
			}
			buffer.append(binaryNumber);
			return buffer.toString();
		}
	}

	public static String appendAdditionalZeroOrOne(String binaryNum) {
		StringBuffer buffer = new StringBuffer();
		int current = 0;
		while (current < binaryNum.length()) {
			if ((binaryNum.length() - current) > 7) {
				buffer.append(0);
			} else {
				buffer.append(1);
			}
			for (int i = current; i < (current + 7); i++) {
				buffer.append(binaryNum.charAt(i));
			}
			current += 7;
		}
		return buffer.toString();
	}

	public static String binaryToHex(String binaryNum) {
		StringBuffer result = new StringBuffer();
		StringBuffer temp;
		int current = 0;
		while (current < binaryNum.length()) {
			temp = new StringBuffer();
			for (int i = current; i < (current + 4); i++) {
				temp.append(binaryNum.charAt(i));
			}
			result.append(binaryBlockOf4ToHex(temp.toString()));
			current += 4;
		}
		return result.toString();
	}

	public static char binaryBlockOf4ToHex(String input) {
		int sum = 0;
		int first = (input.charAt(0) - 48) * 8;
		int second = (input.charAt(1) - 48) * 4;
		int third = (input.charAt(2) - 48) * 2;
		int fourth = (input.charAt(3) - 48);
		sum = first + second + third + fourth;
		if (sum == 10)
			return 'A';
		else if (sum == 11)
			return 'B';
		else if (sum == 12)
			return 'C';
		else if (sum == 13)
			return 'D';
		else if (sum == 14)
			return 'E';
		else if (sum == 15)
			return 'F';
		else
			return (char) (sum + 48);
	}
}