package net.member.action;

import java.io.IOException;
import java.security.PrivateKey;
import java.sql.Timestamp;

import javax.crypto.Cipher;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import net.member.db.MemberBean;
import net.member.db.MemberDAO;

public class MemberJoinAction implements Action {

	@Override
	public ActionForward execute(HttpServletRequest request, HttpServletResponse response) throws Exception {
		
		processRequest(request, response);
		
        // 이동정보 객체 반환
        ActionForward forward = new ActionForward();
        // 확인을 위해서 임시로 페이지 지정
        forward.setPath("./MemberTest.me");
        forward.setRedirect(false);
		
		return forward;
	}
	
	 /**
     * 암호화된 비밀번호를 복호화 한다.
     */
    protected void processRequest(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
    	// 한글처리
    	request.setCharacterEncoding("utf-8");
    	
    	// 파라미터 값 가져오기
        String securedId = request.getParameter("id");
        String securedPass = request.getParameter("pass");
        String name = request.getParameter("name");
        String securedNick = request.getParameter("nick");
        String securedGender = request.getParameter("gender");
        String securedTel = request.getParameter("tel");
        
        // 세션에 저장된 개인키 가져오기
        HttpSession session = request.getSession();
        PrivateKey privateKey = (PrivateKey) session.getAttribute("__rsaPrivateKey__");
        session.removeAttribute("__rsaPrivateKey__"); // 키의 재사용을 막는다. 항상 새로운 키를 받도록 강제.

        if (privateKey == null) {
            throw new RuntimeException("암호화 비밀키 정보를 찾을 수 없습니다.");
        }
        try {
        	// 암호화된 값들 복호화
            String id = decryptRsa(privateKey, securedId);
            String pass = decryptRsa(privateKey, securedPass);
            String nick = decryptRsa(privateKey, securedNick);
            String gender = decryptRsa(privateKey, securedGender);
            String tel = decryptRsa(privateKey, securedTel);
            
            // 빈에 담기
            MemberBean mb = new MemberBean();
            mb.setId(id);
            mb.setPass(pass);
            mb.setName(name);
            mb.setNick(nick);
            mb.setGender(gender);
            mb.setTel(tel);
            Timestamp reg_date = new Timestamp(System.currentTimeMillis());
            mb.setReg_date(reg_date);
            mb.setProfile(null);
            
            // DB작업
            MemberDAO mdao = new MemberDAO();
            mdao.insertMember(mb);
            
            // 세션값 생성
            session.setAttribute("id", id);
            session.setAttribute("nick", nick);
            
        } catch (Exception ex) {
            throw new ServletException(ex.getMessage(), ex);
        }
    }

    private String decryptRsa(PrivateKey privateKey, String securedValue) throws Exception {
        System.out.println("will decrypt : " + securedValue);
        Cipher cipher = Cipher.getInstance("RSA");
        byte[] encryptedBytes = hexToByteArray(securedValue);
        cipher.init(Cipher.DECRYPT_MODE, privateKey);
        byte[] decryptedBytes = cipher.doFinal(encryptedBytes);
        String decryptedValue = new String(decryptedBytes, "utf-8"); // 문자 인코딩 주의.
        return decryptedValue;
    }

    /**
     * 16진 문자열을 byte 배열로 변환한다.
     */
    public static byte[] hexToByteArray(String hex) {
        if (hex == null || hex.length() % 2 != 0) {
            return new byte[]{};
        }

        byte[] bytes = new byte[hex.length() / 2];
        for (int i = 0; i < hex.length(); i += 2) {
            byte value = (byte)Integer.parseInt(hex.substring(i, i + 2), 16);
            bytes[(int) Math.floor(i / 2)] = value;
        }
        return bytes;
    }
	
}
